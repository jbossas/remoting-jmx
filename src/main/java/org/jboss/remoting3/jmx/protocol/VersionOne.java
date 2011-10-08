/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.remoting3.jmx.protocol;

import javax.management.MBeanServerConnection;
import javax.security.auth.Subject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.jmx.RemotingConnectorServer;
import org.jboss.remoting3.jmx.VersionedConnection;
import org.jboss.remoting3.jmx.VersionedProxy;
import org.xnio.AbstractIoFuture;
import org.xnio.IoFuture;
import org.xnio.IoUtils;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class VersionOne {

    private VersionOne() {
    }

    static byte getVersionIdentifier() {
        return 0x01;
    }

    static VersionedConnection getConnection(final Channel channel) throws IOException {
        ClientConnection connection = new ClientConnection(channel);
        connection.start();

        return connection;
    }

    static VersionedProxy getProxy(final Channel channel, final RemotingConnectorServer server) throws IOException {
        ServerProxy proxy = new ServerProxy(channel, server);
        proxy.start();

        return proxy;
    }

}


class ClientConnection implements VersionedConnection {

    private final Channel channel;
    private String connectionId;

    ClientConnection(final Channel channel) {
        this.channel = channel;
    }

    void start() throws IOException {
        sendVersionHeader();
        IoFuture<String> futureConnectionId = ConnectionIdReceiver.getConnectionId(channel);
        IoFuture.Status result = futureConnectionId.await(5, TimeUnit.SECONDS);
        switch (result) {
            case DONE:
                connectionId = futureConnectionId.get();
                break;
            case FAILED:
                throw futureConnectionId.getException();
            default:
                throw new IOException("Unable to obtain connectionId, status=" + result.toString());
        }
    }

    private void sendVersionHeader() throws IOException {
        DataOutputStream dos = new DataOutputStream(channel.writeMessage());
        try {
            dos.writeBytes("JMX");
            dos.writeByte(VersionOne.getVersionIdentifier());
        } finally {
            dos.close();
        }
    }


    public String getConnectionId() {
        if (connectionId == null) {
            throw new IllegalStateException("Connection ID not set");
        }

        return connectionId;
    }

    public MBeanServerConnection getMBeanServerConnection(Subject subject) {
        return null;
    }

    public void close() {

    }

    private static class ConnectionIdReceiver implements Channel.Receiver {

        private static final Logger log = Logger.getLogger(ConnectionIdReceiver.class);

        private final VersionedIoFuture<String> future;

        private ConnectionIdReceiver(VersionedIoFuture<String> future) {
            this.future = future;
        }

        public static IoFuture<String> getConnectionId(final Channel channel) {
            VersionedIoFuture<String> future = new VersionedIoFuture<String>();

            channel.receiveMessage(new ConnectionIdReceiver(future));
            return future;
        }


        public void handleMessage(Channel channel, MessageInputStream messageInputStream) {
            DataInputStream dis = new DataInputStream(messageInputStream);
            try {
                log.infof("Bytes Available %d", dis.available());
                byte[] firstThree = new byte[3];
                dis.read(firstThree);
                log.infof("First Three %s", new String(firstThree));
                if (Arrays.equals(firstThree, "JMX".getBytes()) == false) {
                    throw new IOException("Invalid leading bytes in header.");
                }
                log.infof("Bytes Available %d", dis.available());
                String connectionId = dis.readUTF();
                future.setResult(connectionId);

            } catch (IOException e) {
                future.setException(e);
            } finally {
                IoUtils.safeClose(dis);
            }

        }

        public void handleError(Channel channel, IOException e) {
            future.setException(e);
        }

        public void handleEnd(Channel channel) {
            future.setException(new IOException("Channel ended"));
        }

    }

    // TODO - Cleaner future handling as used in a couple of locations.
    private static class VersionedIoFuture<T> extends AbstractIoFuture<T> {

        @Override
        protected boolean setResult(T result) {
            return super.setResult(result);
        }

        @Override
        protected boolean setException(IOException exception) {
            return super.setException(exception);
        }

    }
}

class ServerProxy implements VersionedProxy {

    private static final Logger log = Logger.getLogger(ServerProxy.class);

    private final Channel channel;
    private final RemotingConnectorServer server;
    private UUID connectionId;

    ServerProxy(final Channel channel, final RemotingConnectorServer server) {
        this.channel = channel;
        this.server = server;
    }

    void start() throws IOException {
        // Create a connection ID
        connectionId = UUID.randomUUID();
        log.infof("Created connectionID %s", connectionId.toString());
        // Send ID to client
        sendConnectionId();
        // Inform server the connection is now open
        server.connectionOpened(this);
    }

    private void sendConnectionId() throws IOException {
        DataOutputStream dos = new DataOutputStream(channel.writeMessage());
        try {
            dos.writeBytes("JMX");
            dos.writeUTF(connectionId.toString());
        } finally {
            dos.close();
            log.infof("Written connectionId %s", connectionId.toString());
        }
    }

    public String getConnectionId() {
        return connectionId.toString();
    }


}
