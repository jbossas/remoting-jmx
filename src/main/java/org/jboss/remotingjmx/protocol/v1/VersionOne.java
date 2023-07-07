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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.Capability;
import org.jboss.remotingjmx.ServerMessageInterceptor;
import org.jboss.remotingjmx.VersionedConnection;
import org.jboss.remotingjmx.WrappedMBeanServerConnection;

/**
 * The entry point to VersionOne
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class VersionOne {

    private VersionOne() {
    }

    public static byte getVersionIdentifier() {
        return 0x01;
    }

    public static Set<Capability> getCapabilites() {
        return Collections.emptySet();
    }

    public static VersionedConnection getConnection(final Channel channel, final Map<String, ?> environment) throws IOException {
        ClientConnection connection = new ClientConnection(channel, environment);
        connection.start();

        return connection;
    }

    public static void startServer(final Channel channel, final WrappedMBeanServerConnection server, final Executor executor,
            final ServerMessageInterceptor serverMessageInterceptor) throws IOException {
        ServerProxy proxy = new ServerProxy(channel, server, executor, serverMessageInterceptor);
        proxy.start();
    }

}
