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
package org.jboss.remotingjmx;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Endpoint;

/**
 * A JMXConnectorServer to handle the server side of the lifecycle relating to making the provided MBeanServer accessible using
 * JBoss Remoting.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RemotingConnectorServer extends JMXConnectorServer {

    private static final Logger log = Logger.getLogger(RemotingConnectorServer.class);

    private final DelegatingRemotingConnectorServer connectorServer;

    public RemotingConnectorServer(final MBeanServer mbeanServer, final Endpoint endpoint) {
        this(mbeanServer, endpoint, Collections.EMPTY_MAP);
    }

    public RemotingConnectorServer(final MBeanServer mbeanServer, final Endpoint endpoint,
            ServerMessageInterceptorFactory serverMessageInterceptorFactory) {
        this(mbeanServer, endpoint, Executors.newCachedThreadPool(), Collections.EMPTY_MAP, serverMessageInterceptorFactory);
    }

    public RemotingConnectorServer(final MBeanServer mbeanServer, final Endpoint endpoint, final Map<String, ?> environment) {
        this(mbeanServer, endpoint, Executors.newCachedThreadPool(), environment, null);
    }

    public RemotingConnectorServer(final MBeanServer mbeanServer, final Endpoint endpoint, final Map<String, ?> environment,
            ServerMessageInterceptorFactory serverMessageInterceptorFactory) {
        this(mbeanServer, endpoint, Executors.newCachedThreadPool(), environment, serverMessageInterceptorFactory);
    }

    public RemotingConnectorServer(final MBeanServer mbeanServer, final Endpoint endpoint, Executor executor) {
        this(mbeanServer, endpoint, executor, Collections.EMPTY_MAP, null);
    }

    public RemotingConnectorServer(final MBeanServer mbeanServer, final Endpoint endpoint, Executor executor,
            final Map<String, ?> environment, ServerMessageInterceptorFactory serverMessageInterceptorFactory) {
        super(mbeanServer);
        MBeanServerManager serverManager = new MBeanServerManager() {
            private final WrappedMBeanServerConnection connection = new WrappedMBeanServerConnection() {

                @Override
                public MBeanServerConnection getMBeanServerConnection() {
                    return RemotingConnectorServer.this.getMBeanServer();
                }

                @Override
                public void connectionOpened(VersionedProxy proxy) {
                    String connectionId = proxy.getConnectionId();
                    log.debugf("Connection '%s' now opened.", connectionId);
                    RemotingConnectorServer.this.connectionOpened(connectionId, "", null);
                }

                @Override
                public void connectionClosed(VersionedProxy proxy) {
                    String connectionId = proxy.getConnectionId();
                    log.debugf("Connection '%s' now opened.", connectionId);
                    RemotingConnectorServer.this.connectionClosed(connectionId, "", null);
                }
            };

            @Override
            public WrappedMBeanServerConnection getDefaultMBeanServer() {
                return connection;
            }

            @Override
            public WrappedMBeanServerConnection getMBeanServer(Map<String, String> parameters) {
                if (parameters.isEmpty()) {
                    return getDefaultMBeanServer();
                }
                // Only a single server do don't support parameters.
                return null;
            }
        };

        connectorServer = new DelegatingRemotingConnectorServer(serverManager, endpoint, executor, environment,
                serverMessageInterceptorFactory);
    }

    /*
     * Methods from JMXConnectorServerMBean
     */

    public void start() throws IOException {
        connectorServer.start();
    }

    public void stop() throws IOException {
        connectorServer.stop();
    }

    public boolean isActive() {
        return connectorServer.isActive();
    }

    public JMXServiceURL getAddress() {
        return connectorServer.getAddress();
    }

    public Map<String, ?> getAttributes() {
        return connectorServer.getAttributes();
    }

}
