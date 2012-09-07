/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.remotingjmx.protocol.v2;

import static org.jboss.remotingjmx.protocol.v2.Constants.BEGIN;
import static org.jboss.remotingjmx.protocol.v2.Constants.RESPONSE_MASK;
import static org.jboss.remotingjmx.protocol.v2.Constants.SET_KEY_PAIR;
import static org.jboss.remotingjmx.protocol.v2.Constants.STRING;
import static org.jboss.remotingjmx.protocol.v2.Constants.VOID;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXServiceURL;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.VersionedConnection;
import org.xnio.IoFuture;

/**
 * Class responsible for the initial parameter exchange on the connection before we 'begin' and allow interoperability with the
 * target MBeanServer.
 *
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ParameterConnection extends ClientCommon {

    private static final Logger log = Logger.getLogger(ParameterConnection.class);

    private final Channel channel;
    private final Map<String, ?> environment;
    private final ClientRequestManager clientRequestManager;
    private final ClientExecutorManager clientExecutorManager;
    private final JMXServiceURL serviceUrl;
    // Registry of handlers for the incoming messages.
    private final Map<Byte, Common.MessageHandler> handlerRegistry;

    ParameterConnection(Channel channel, final Map<String, ?> environment, final JMXServiceURL serviceUrl) {
        super(channel, environment);
        this.channel = channel;
        this.environment = environment;
        this.clientRequestManager = new ClientRequestManager();
        this.clientExecutorManager = new ClientExecutorManager(environment);
        this.serviceUrl = serviceUrl;
        this.handlerRegistry = createHandlerRegistry();
    }

    private Map<Byte, Common.MessageHandler> createHandlerRegistry() {
        Map<Byte, Common.MessageHandler> registry = new HashMap<Byte, Common.MessageHandler>();
        registry.put((byte) (SET_KEY_PAIR ^ RESPONSE_MASK), new MarshalledResponseHandler<Void>(VOID));
        registry.put((byte) (BEGIN ^ RESPONSE_MASK), new BeginResponseHandler());

        return Collections.unmodifiableMap(registry);
    }

    @Override
    Map<Byte, Common.MessageHandler> getHandlerRegistry() {
        return handlerRegistry;
    }

    @Override
    protected ClientRequestManager getClientRequestManager() {
        return clientRequestManager;
    }

    @Override
    protected ClientExecutorManager getClientExecutorManager() {
        return clientExecutorManager;
    }

    VersionedConnection getConnection() throws IOException {
        sendVersionHeader();

        IoFuture<Void> futureWelcome = WelcomeMessageReceiver.awaitWelcomeMessage(channel);
        IoFuture.Status result = futureWelcome.await(timeoutSeconds, TimeUnit.SECONDS);
        switch (result) {
            case DONE:
                // Set this first as something will need to start handling the response messages.
                channel.receiveMessage(new MessageReceiver());
                break;
            case FAILED:
                throw futureWelcome.getException();
            default:
                throw new IOException("Unable to obtain connectionId, status=" + result.toString());
        }

        sendKeyPairs();
        String connectionId = begin();

        ClientConnection cc = new ClientConnection(channel, environment, clientRequestManager, clientExecutorManager,
                connectionId);
        cc.start();

        return cc;
    }

    private void sendKeyPairs() throws IOException {
        String path = serviceUrl.getURLPath();
        if (path.contains("?")) {
            String parameters = path.substring(path.indexOf("?") + 1);
            String[] pairs = parameters.split(",");
            for (String currentPair : pairs) {
                String[] keyValue = currentPair.split("=");
                if (keyValue.length == 2) {
                    setKeyPair(keyValue[0], keyValue[1]);
                } else {
                    throw new IOException(String.format("Unable to parse key pairs from '%s'", parameters));
                }
            }
        }
    }

    private void sendVersionHeader() throws IOException {
        write(new MessageWriter() {
            @Override
            public void write(DataOutput output) throws IOException {
                output.writeBytes("JMX");
                output.writeByte(VersionTwo.getVersionIdentifier());
            }
        });
    }

    private void setKeyPair(final String key, final String value) throws IOException {
        VersionedIoFuture<TypeExceptionHolder<Void>> future = new VersionedIoFuture<TypeExceptionHolder<Void>>();
        final int correlationId = clientRequestManager.reserveNextCorrelationId(future);
        try {
            write(new MessageWriter() {

                @Override
                public void write(DataOutput output) throws IOException {
                    output.writeByte(SET_KEY_PAIR);
                    output.writeInt(correlationId);

                    output.writeByte(STRING);
                    output.writeUTF(key);
                    output.writeByte(STRING);
                    output.writeUTF(value);
                }
            });

            log.tracef("[%d] unregisterMBean - Request Sent", correlationId);

            IoFuture.Status result = future.await(timeoutSeconds, TimeUnit.SECONDS);
            switch (result) {
                case FAILED:
                    throw future.getException();
                case DONE:
                    TypeExceptionHolder<Void> response = future.get();
                    if (response.e == null) {
                        return;
                    }

                    throw toIoException(response.e);
                default:
                    throw new IOException("Unable to invoke unregisterMBean, status=" + result.toString());
            }
        } finally {
            clientRequestManager.releaseCorrelationId(correlationId);
        }
    }

    private String begin() throws IOException {
        VersionedIoFuture<TypeExceptionHolder<String>> future = new VersionedIoFuture<TypeExceptionHolder<String>>();
        final int correlationId = clientRequestManager.reserveNextCorrelationId(future);
        try {
            write(new MessageWriter() {

                @Override
                public void write(DataOutput output) throws IOException {
                    output.writeByte(BEGIN);
                    output.writeInt(correlationId);
                }
            });

            log.tracef("[%d] begin - Request Sent", correlationId);

            IoFuture.Status result = future.await(timeoutSeconds, TimeUnit.SECONDS);
            switch (result) {
                case FAILED:
                    throw future.getException();
                case DONE:
                    TypeExceptionHolder<String> response = future.get();
                    if (response.e == null) {
                        return response.value;
                    }
                    throw toIoException(response.e);
                default:
                    throw new IOException("Unable to invoke begin, status=" + result.toString());
            }
        } finally {
            clientRequestManager.releaseCorrelationId(correlationId);
        }

    }

    private class BeginResponseHandler extends StringResponseHandler implements ClientCommon.MessageHandler {

        // Whatever the response we are finished receiving until ClientConnection starts.

        @Override
        public boolean endReceiveLoop() {
            return true;
        }

    }

}
