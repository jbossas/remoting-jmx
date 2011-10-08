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

import static org.jboss.remoting3.jmx.Constants.SNAPSHOT;
import static org.jboss.remoting3.jmx.Constants.STABLE;
import static org.jboss.remoting3.jmx.Constants.JMX;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;

import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.jmx.protocol.Versions;
import org.xnio.AbstractIoFuture;
import org.xnio.IoFuture;
import org.xnio.IoUtils;

/**
 * The VersionedConnectionFactory to negotiate the version on the client side and
 * return an appropriate VersionedConnection for the negotiated version.
 * <p/>
 * As the only entry point to this class is the create method and as that method creates
 * a new instance for each call it is guaranteed there will not be concurrent
 * negotiations occurring.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class VersionedConectionFactory {

    private static final Logger log = Logger.getLogger(VersionedConectionFactory.class);

    private VersionedIoFuture<VersionedConnection> future;
    private Channel channel;

    private VersionedConectionFactory(final VersionedIoFuture<VersionedConnection> future, final Channel channel) {
        this.future = future;
        this.channel = channel;
    }


    static IoFuture<VersionedConnection> createMBeanServerConnection(final Channel channel) {
        VersionedIoFuture<VersionedConnection> future = new VersionedIoFuture<VersionedConnection>() {
        };

        VersionedConectionFactory factory = new VersionedConectionFactory(future, channel);
        factory.startVersionNegotiation();

        return future;
    }

    private void startVersionNegotiation() {
        channel.receiveMessage(new ClientVersionReceiver());
    }

    /**
     * A Channel.Receiver to handle the initial negotiation of the version to use.
     * <p/>
     * The client will initially receive a list of versions supported by the server, from this list
     * the highest version supported by the client should be selected - this will allow older clients
     * to operate against later servers.
     */
    private class ClientVersionReceiver implements org.jboss.remoting3.Channel.Receiver {

        /**
         * Verify the header received, confirm to the server the version selected, create the
         * client channel receiver and assign it to the channel.
         */
        public void handleMessage(org.jboss.remoting3.Channel channel, MessageInputStream messageInputStream) {
            DataInputStream dis = new DataInputStream(messageInputStream);
            try {
                log.infof("Bytes Available %d", dis.available());
                byte[] firstThree = new byte[3];
                dis.read(firstThree);
                log.infof("First Three %s", new String(firstThree));
                if (Arrays.equals(firstThree, JMX) == false) {
                    throw new IOException("Invalid leading bytes in header.");
                }
                log.infof("Bytes Available %d", dis.available());
                int versionCount = dis.read();
                log.infof("Expecting %d versions", versionCount);
                byte[] versions = new byte[versionCount];
                dis.read(versions);

                StringBuffer sbVersions = new StringBuffer("Versions ");
                for (byte current : versions) {
                    sbVersions.append(" 0x0").append(current);
                }
                log.info(sbVersions);

                byte stability = dis.readByte();
                switch (stability) {
                    case STABLE:
                        log.info("Calling a stable server");
                        break;
                    case SNAPSHOT:
                        log.warn("Calling a snapshot server");
                        break;
                    default:
                        throw new IOException("Unrecognised stability value.");
                }

                // Find the highest version.
                byte highest = 0x00;
                for (byte current : versions) {
                    if (current > highest) {
                        highest = current;
                    }
                }

                future.setResult(Versions.getVersionedConnection(highest, channel));
            } catch (IOException e) {
                log.error("Unable to negotiate connection.", e);
                future.setException(e);
            } finally {
                IoUtils.safeClose(dis);
            }
        }

        public void handleError(org.jboss.remoting3.Channel channel, IOException e) {
            log.error("Error on channel", e);
            // TODO - At this point the connection is still opening so probably don't want to completely close future interatcion.
        }

        public void handleEnd(org.jboss.remoting3.Channel channel) {
            log.error("Channel ended.");
            // TODO - At this point the connection is still opening so probably don't want to completely close future interatcion.
        }

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
