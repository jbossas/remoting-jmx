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
package org.jboss.remotingjmx.protocol.v1;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.Capability;
import org.jboss.remotingjmx.ServerMessageEventHandler;
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
            final ServerMessageEventHandler serverMessageEventHandler) throws IOException {
        ServerProxy proxy = new ServerProxy(channel, server, executor, serverMessageEventHandler);
        proxy.start();
    }

}
