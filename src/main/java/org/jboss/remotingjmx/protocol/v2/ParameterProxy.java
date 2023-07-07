/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.remotingjmx.protocol.v2;

import static org.jboss.remotingjmx.protocol.v2.Constants.BEGIN;
import static org.jboss.remotingjmx.protocol.v2.Constants.SET_KEY_PAIR;
import static org.jboss.remotingjmx.protocol.v2.Constants.STRING;

import java.io.DataInput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.MBeanServerManager;
import org.jboss.remotingjmx.ServerMessageInterceptor;
import org.jboss.remotingjmx.WrappedMBeanServerConnection;

/**
 * The server side proxy responsible for handling the initial setKeyPair requests before begin is called and an MBeanServer
 * selected.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ParameterProxy extends ServerCommon {

    private static final Logger log = Logger.getLogger(ParameterProxy.class);

    private final Channel channel;
    private final Map<Byte, Common.MessageHandler> registry;
    private final Map<String, String> keyPairs = new HashMap<String, String>();
    private final MBeanServerManager mbeanServerManager;
    private final Executor executor;
    private final ServerMessageInterceptor serverMessageInterceptor;

    ParameterProxy(Channel channel, MBeanServerManager mbeanServerManager, Executor executor,
            ServerMessageInterceptor serverMessageInterceptor) {
        super(channel, executor, serverMessageInterceptor);
        this.channel = channel;
        this.executor = executor;
        this.mbeanServerManager = mbeanServerManager;
        registry = createHandlerRegistry();
        this.serverMessageInterceptor = serverMessageInterceptor;
    }

    private Map<Byte, Common.MessageHandler> createHandlerRegistry() {
        Map<Byte, Common.MessageHandler> registry = new HashMap<Byte, Common.MessageHandler>();
        registry.put(SET_KEY_PAIR, new SetKeyPairHandler());
        registry.put(BEGIN, new BeginHandler());

        return Collections.unmodifiableMap(registry);
    }

    void start() throws IOException {
        // Create a connection ID
        log.debugf("Created connection - ID to be established after parameter negotiation.");
        // Send the welcome message.
        sendWelcomeMessage();

        channel.receiveMessage(new MessageReciever());
    }

    @Override
    Map<Byte, Common.MessageHandler> getHandlerRegistry() {
        return registry;
    }

    @Override
    void end() {
    }

    private class SetKeyPairHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, final int correlationId) throws IOException {
            log.trace("SetKeyPair");

            byte paramType = input.readByte();
            if (paramType != STRING) {
                throw new IOException("Unexpected paramType");
            }

            String name = input.readUTF();

            paramType = input.readByte();
            if (paramType != STRING) {
                throw new IOException("Unexpected paramType");
            }

            String value = input.readUTF();

            keyPairs.put(name, value);

            writeResponse(SET_KEY_PAIR, correlationId);
        }
    }

    private class BeginHandler extends MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("Begin");

            WrappedMBeanServerConnection mbeanServer = mbeanServerManager.getMBeanServer(keyPairs);

            if (mbeanServer != null) {
                ServerProxy server = new ServerProxy(channel, mbeanServer, executor, serverMessageInterceptor);
                server.start();

                String connectionId = server.getConnectionId();
                writeResponse(connectionId, BEGIN, correlationId);
            } else {
                // No MBeanServer was located ;-(
                throw new IOException("No MBeanServer identified from the specified parameters.");
            }
        }

        @Override
        boolean endReceiveLoop() {
            // This handler is responsible for setting the next Receiver.
            return true;
        }

    }

}
