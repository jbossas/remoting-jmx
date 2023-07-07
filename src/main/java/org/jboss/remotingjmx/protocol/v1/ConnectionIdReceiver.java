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

package org.jboss.remotingjmx.protocol.v1;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoFuture;
import org.xnio.IoUtils;

/**
 * The ConnectionId Receiver used by the client.
 *
 * This is a special receiver as unlike the other messages blocking on the result is desired to ensure the connection is
 * established before any client interactions occur.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class ConnectionIdReceiver implements Channel.Receiver {

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
            log.tracef("Bytes Available %d", dis.available());
            byte[] firstThree = new byte[3];
            dis.read(firstThree);
            log.tracef("First Three %s", new String(firstThree));
            if (Arrays.equals(firstThree, "JMX".getBytes()) == false) {
                throw new IOException("Invalid leading bytes in header.");
            }
            log.tracef("Bytes Available %d", dis.available());
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