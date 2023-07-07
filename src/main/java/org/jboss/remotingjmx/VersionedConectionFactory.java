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

package org.jboss.remotingjmx;

import static org.jboss.remotingjmx.Constants.JMX;
import static org.jboss.remotingjmx.Constants.JMX_BYTES;
import static org.jboss.remotingjmx.Constants.SNAPSHOT;
import static org.jboss.remotingjmx.Constants.STABLE;
import static org.jboss.remotingjmx.Util.getTimeoutValue;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXServiceURL;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remotingjmx.Util.Timeout;
import org.jboss.remotingjmx.protocol.CancellableDataOutputStream;
import org.jboss.remotingjmx.protocol.Versions;
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
 * @author <a href="mailto:brad.maxwell@redhat.com">Brad Maxwell</a>
 */
class VersionedConectionFactory {

    private static final Logger log = Logger.getLogger(VersionedConectionFactory.class);

    static VersionedConnection createVersionedConnection(final Channel channel, final Map<String, ?> environment,
            final JMXServiceURL serviceURL) throws IOException {
        // We don't want to start chaining the use of IoFutures otherwise multiple threads are tied up
        // for a single negotiation process so negotiate the connection sequentially.

        IoFuture<InitialHeader> futureHeader = ClientVersionReceiver.getInitialHeader(channel);
        IoFuture.Status result = futureHeader.await(getTimeoutValue(Timeout.VERSIONED_CONNECTION, environment), TimeUnit.SECONDS);
        switch (result) {
            case DONE:
                break;
            case FAILED:
                throw futureHeader.getException();
            default:
                throw new IOException("Timeout out waiting for header, status=" + result.toString());
        }

        InitialHeader header = futureHeader.get();

        Versions versions = new Versions(environment);
        Set<Byte> supportedVersions = versions.getSupportedVersions(getRequiredCapabilities(serviceURL));

        // Find the highest version. - By this point the exceptional handling of version 0x00 will have completed.
        byte highest = 0x00;
        for (byte current : header.versions) {
            // Only accept it if it is one of the supported versions otherwise ignore as noise.
            if (supportedVersions.contains(current) && current > highest) {
                highest = current;
            }
        }

        if (highest == 0x00) {
            throw new IllegalStateException("No matching supported protocol version found.");
        }

        // getVersionedConnection may also make use of an IoFuture but our previous use of one has ended.
        return versions.getVersionedConnection(highest, channel, serviceURL);
    }

    private static Capability[] getRequiredCapabilities(final JMXServiceURL serviceURL) {
        Set<Capability> requiredCapabilities = new HashSet<Capability>();
        String path = serviceURL.getURLPath();
        // The ? delimiter is only used if there will be subsequent parameters.
        if (path.contains("?")) {
            requiredCapabilities.add(Capability.PASS_PARAMETERS);
        }

        return requiredCapabilities.toArray(new Capability[requiredCapabilities.size()]);
    }

    /**
     * A Channel.Receiver to receive the list of versions supported by the remote server.
     */
    private static class ClientVersionReceiver implements org.jboss.remoting3.Channel.Receiver {

        private final VersionedIoFuture<InitialHeader> future;
        private boolean expectServerVersion = false;

        private ClientVersionReceiver(VersionedIoFuture<InitialHeader> future) {
            this.future = future;
        }

        public static IoFuture<InitialHeader> getInitialHeader(final Channel channel) {
            VersionedIoFuture<InitialHeader> future = new VersionedIoFuture<InitialHeader>();

            channel.receiveMessage(new ClientVersionReceiver(future));

            return future;
        }

        private void sendVersionZeroHeader(Channel channel) throws IOException {
            log.debug("Selecting version 0x00 to receive full version list.");
            CancellableDataOutputStream dos = new CancellableDataOutputStream(channel.writeMessage());
            try {
                dos.writeBytes(JMX);
                dos.writeByte(0x00);
                String remotingJMXVersion = Version.getVersionString();
                byte[] versionBytes = remotingJMXVersion.getBytes("UTF-8");
                dos.writeInt(versionBytes.length);
                dos.write(versionBytes);
            } catch (IOException e) {
                dos.cancel();
                throw e;
            } finally {
                IoUtils.safeClose(dos);
            }
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
                if (Arrays.equals(firstThree, JMX_BYTES) == false) {
                    throw new IOException("Invalid leading bytes in header.");
                }
                log.tracef("Bytes Available %d", dis.available());
                int versionCount = dis.readInt();
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

                String serverVersion = null;
                if (expectServerVersion) {
                    int length = dis.readInt();
                    byte[] versionBytes = new byte[length];
                    dis.read(versionBytes);
                    serverVersion = new String(versionBytes, "UTF-8");
                    log.debugf("Server version %s", serverVersion);
                }

                for (byte current : versions) {
                    if (current == 0x00) {
                        sendVersionZeroHeader(channel);
                        expectServerVersion = true;
                        channel.receiveMessage(this);
                        return;
                    }
                }

                InitialHeader ih = new InitialHeader();
                ih.versions = versions;
                ih.stability = stability;
                ih.serverVersion = serverVersion;
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
        private String serverVersion;
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
