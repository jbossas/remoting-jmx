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
