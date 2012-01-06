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
package org.jboss.remoting3.jmx;

import static org.jboss.remoting3.jmx.Constants.JMX;
import static org.jboss.remoting3.jmx.Constants.SNAPSHOT;
import static org.jboss.remoting3.jmx.Constants.STABLE;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.jmx.protocol.Versions;
import org.xnio.AbstractIoFuture;
import org.xnio.IoFuture;
import org.xnio.IoUtils;

/**
 * The VersionedConnectionFactory to negotiate the version on the client side and return an appropriate VersionedConnection for
 * the negotiated version.
 * <p/>
 * As the only entry point to this class is the create method and as that method creates a new instance for each call it is
 * guaranteed there will not be concurrent negotiations occurring.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class VersionedConectionFactory {

    private static final Logger log = Logger.getLogger(VersionedConectionFactory.class);

    static VersionedConnection createVersionedConnection(final Channel channel) throws IOException {
        // We don't want to start chaining the use of IoFutures otherwise multiple threads are tied up
        // for a single negotiation process so negotiate the connection sequentially.

        IoFuture<InitialHeader> futureHeader = ClientVersionReceiver.getInitialHeader(channel);
        IoFuture.Status result = futureHeader.await(5, TimeUnit.SECONDS);
        switch (result) {
            case DONE:
                break;
            case FAILED:
                throw futureHeader.getException();
            default:
                throw new IOException("Timeout out waiting for header, status=" + result.toString());
        }

        InitialHeader header = futureHeader.get();

        // Find the highest version.
        byte highest = 0x00;
        for (byte current : header.versions) {
            if (current > highest) {
                highest = current;
            }
        }

        // getVersionedConnection may also make use of an IoFuture but our previous use of one has ended.
        return Versions.getVersionedConnection(highest, channel);
    }

    /**
     * A Channel.Receiver to receive the list of versions supported by the remote server.
     */
    private static class ClientVersionReceiver implements org.jboss.remoting3.Channel.Receiver {

        private final VersionedIoFuture<InitialHeader> future;

        private ClientVersionReceiver(VersionedIoFuture<InitialHeader> future) {
            this.future = future;
        }

        public static IoFuture<InitialHeader> getInitialHeader(final Channel channel) {
            VersionedIoFuture<InitialHeader> future = new VersionedIoFuture<InitialHeader>();

            channel.receiveMessage(new ClientVersionReceiver(future));

            return future;
        }

        /**
         * Verify the header received, confirm to the server the version selected, create the client channel receiver and assign
         * it to the channel.
         */
        public void handleMessage(org.jboss.remoting3.Channel channel, MessageInputStream messageInputStream) {
            DataInputStream dis = new DataInputStream(messageInputStream);
            try {
                log.tracef("Bytes Available %d", dis.available());
                byte[] firstThree = new byte[3];
                dis.read(firstThree);
                log.tracef("First Three %s", new String(firstThree));
                if (Arrays.equals(firstThree, JMX) == false) {
                    throw new IOException("Invalid leading bytes in header.");
                }
                log.tracef("Bytes Available %d", dis.available());
                int versionCount = dis.read();
                log.tracef("Expecting %d versions", versionCount);
                byte[] versions = new byte[versionCount];
                dis.read(versions);

                if (log.isDebugEnabled()) {
                    StringBuffer sbVersions = new StringBuffer("Versions ");
                    for (byte current : versions) {
                        sbVersions.append(" 0x0").append(current);
                    }
                    log.debugf("Available version (%s)", sbVersions);
                }

                byte stability = dis.readByte();
                switch (stability) {
                    case STABLE:
                        log.debug("Calling a stable server");
                        break;
                    case SNAPSHOT:
                        log.warn("Calling a snapshot server");
                        break;
                    default:
                        throw new IOException("Unrecognised stability value.");
                }

                InitialHeader ih = new InitialHeader();
                ih.versions = versions;
                ih.stability = stability;
                future.setResult(ih);
            } catch (IOException e) {
                log.error("Unable to negotiate connection.", e);
                future.setException(e);
            } finally {
                IoUtils.safeClose(dis);
            }
        }

        public void handleError(org.jboss.remoting3.Channel channel, IOException e) {
            log.error("Error on channel", e);
            future.setException(e);
        }

        public void handleEnd(org.jboss.remoting3.Channel channel) {
            log.error("Channel ended.");
            future.setException(new IOException("Channel ended"));
        }

    }

    private static class InitialHeader {
        private byte[] versions;
        private byte stability;
    }

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
