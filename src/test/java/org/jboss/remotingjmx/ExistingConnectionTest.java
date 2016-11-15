/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.remotingjmx.Constants.REMOTE_SCHEME;
import static org.jboss.remotingjmx.common.Constants.PROTOCOL;
import static org.jboss.remotingjmx.common.JMXRemotingServer.DEFAULT_PORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.xnio.Options.SASL_POLICY_NOANONYMOUS;
import static org.xnio.Options.SASL_POLICY_NOPLAINTEXT;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.SSLSession;

import org.jboss.remoting3.Attachments;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.ConnectionPeerIdentity;
import org.jboss.remoting3.ConnectionPeerIdentityContext;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remotingjmx.common.JMXRemotingServer;
import org.jboss.remotingjmx.common.JMXRemotingServer.JMXRemotingConfig;
import org.jboss.remotingjmx.common.MyBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.wildfly.security.auth.AuthenticationException;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Property;
import org.xnio.Sequence;

/**
 * Test case to test establishing a connection using an existing Remoting Connection.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Ignore // cannot work as written for now
public class ExistingConnectionTest extends AbstractTestBase {

    @Before
    @Override
    public void connect() throws IOException {
    }

    @After
    @Override
    public void disconnect() throws IOException {
    }

    /**
     * Test a normal connecton where the client side connects to the server.
     */
    @Test
    public void testConnectionToServer() throws Exception {
        assertNull(super.connector);

        Endpoint endpoint = Endpoint.getCurrent();
        // open a connection

        final IoFuture<Connection> futureConnection = AuthenticationContext.empty().with(
            MatchRule.ALL,
            AuthenticationConfiguration.EMPTY.useAnonymous().allowSaslMechanisms("ANONYMOUS")
        ).runExFunction(v -> endpoint.connect(new URI(REMOTE_SCHEME, null, bindAddress,
                DEFAULT_PORT, null, null, null), getOptionMap()), null);
        IoFuture.Status result = futureConnection.await(5, TimeUnit.SECONDS);

        Connection connection;
        if (result == IoFuture.Status.DONE) {
            connection = futureConnection.get();
        } else if (result == IoFuture.Status.FAILED) {
            throw futureConnection.getException();
        } else {
            throw new RuntimeException("Operation failed with status " + result);
        }

        actualJMXTest(connection);

        connection.close();
    }

    /**
     * Test the reverse connection i.e. the process running the MBeanServer is responsible for establishing the Remoting
     * Connection but then the JMX connection is in the opposite direction.
     */
    @Test
    public void testConnectionToClient() throws Exception {
        MBeanServer secondMbeanServer = MBeanServerFactory.createMBeanServer(DEFAULT_DOMAIN + "2");

        JMXRemotingConfig config = new JMXRemotingConfig();
        config.mbeanServer = secondMbeanServer;
        config.host = bindAddress;
        config.port = DEFAULT_PORT + 1;

        JMXRemotingServer remotingServer = new JMXRemotingServer(config);
        remotingServer.start();
        try {
            TestOpenListener openListener = new TestOpenListener();
            remotingServer.getEndpoint().registerService("TestChannel", openListener, OptionMap.EMPTY);

            // At this point use the Endpoint within JMXRemotingServer to establish a connection to this second server.
            final IoFuture<Connection> futureConnection = Endpoint.getCurrent().connect(
                    new URI(REMOTE_SCHEME, null, bindAddress, DEFAULT_PORT + 1, null, null, null), getOptionMap());
            IoFuture.Status result = futureConnection.await(5, TimeUnit.SECONDS);

            Connection connection;
            if (result == IoFuture.Status.DONE) {
                connection = futureConnection.get();
            } else if (result == IoFuture.Status.FAILED) {
                throw futureConnection.getException();
            } else {
                throw new RuntimeException("Operation failed with status " + result);
            }
            try {
                final IoFuture<Channel> futureChannel = connection.openChannel("TestChannel", OptionMap.EMPTY);
                result = futureChannel.await(5, TimeUnit.SECONDS);
                Channel channel;
                if (result == IoFuture.Status.DONE) {
                    channel = futureChannel.get();
                } else if (result == IoFuture.Status.FAILED) {
                    throw new IOException(futureChannel.getException());
                } else {
                    throw new RuntimeException("Operation failed with status " + result);
                }

                assertNotNull(channel);
                Connection conFromServer = openListener.getConnection();
                System.out.println("About to asert");
                assertNotNull(conFromServer);

                // At this point we can use conFromServer to establish a reverse connection.
                actualJMXTest(conFromServer);
            } finally {
                connection.close();
            }
        } finally {
            remotingServer.stop();
        }
    }

    private void actualJMXTest(final Connection connection) throws Exception {
        Map<String, Object> env = new HashMap<String, Object>(1);
        WrappedConnection wrapped = new WrappedConnection(connection);
        env.put(Connection.class.getName(), wrapped);
        JMXServiceURL serviceURL = new JMXServiceURL(PROTOCOL, "nowhere", 1);
        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);

        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testCreate");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection mbeanCon = connector.getMBeanServerConnection();
        assertTrue(wrapped.channelOpened);

        try {
            assertFalse(mbeanCon.isRegistered(beanName));
            ObjectInstance instance = mbeanCon.createMBean(MyBean.class.getName(), beanName);
            assertEquals(beanName, instance.getObjectName());
            assertEquals(MyBean.class.getName(), instance.getClassName());
            assertTrue(mbeanCon.isRegistered(beanName));
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }

        connector.close();
    }

    private OptionMap getOptionMap() {
        OptionMap.Builder builder = OptionMap.builder();
        builder.set(SASL_POLICY_NOANONYMOUS, Boolean.FALSE);
        builder.set(SASL_POLICY_NOPLAINTEXT, Boolean.FALSE);

        List<Property> tempProperties = new ArrayList<Property>(1);
        tempProperties.add(Property.of("jboss.sasl.local-user.quiet-auth", "true"));
        builder.set(Options.SASL_PROPERTIES, Sequence.of(tempProperties));

        builder.set(Options.SSL_ENABLED, true);
        builder.set(Options.SSL_STARTTLS, true);

        return builder.getMap();
    }

    private class WrappedConnection implements Connection {

        private final Connection wrapped;
        private boolean channelOpened = false;

        private WrappedConnection(Connection wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public org.jboss.remoting3.HandleableCloseable.Key addCloseHandler(CloseHandler<? super Connection> arg0) {
            throw new IllegalStateException("No close handling expected.");
        }

        @Override
        public void awaitClosed() throws InterruptedException {
            throw new IllegalStateException("No close handling expected.");
        }

        @Override
        public void awaitClosedUninterruptibly() {
            throw new IllegalStateException("No close handling expected.");
        }

        @Override
        public void close() throws IOException {
            throw new IllegalStateException("No close handling expected.");
        }

        @Override
        public void closeAsync() {
            throw new IllegalStateException("No close handling expected.");
        }

        @Override
        public Attachments getAttachments() {
            return wrapped.getAttachments();
        }

        @Override
        public Endpoint getEndpoint() {
            return wrapped.getEndpoint();
        }

        public URI getPeerURI() {
            return wrapped.getPeerURI();
        }

        public SecurityIdentity getLocalIdentity() {
            return wrapped.getLocalIdentity();
        }

        public SecurityIdentity getLocalIdentity(final int id) {
            return wrapped.getLocalIdentity(id);
        }

        public int getPeerIdentityId() throws AuthenticationException {
            return wrapped.getPeerIdentityId();
        }

        public SocketAddress getLocalAddress() {
            return wrapped.getLocalAddress();
        }

        public <S extends SocketAddress> S getLocalAddress(final Class<S> type) {
            return wrapped.getLocalAddress(type);
        }

        public SocketAddress getPeerAddress() {
            return wrapped.getPeerAddress();
        }

        public <S extends SocketAddress> S getPeerAddress(final Class<S> type) {
            return wrapped.getPeerAddress(type);
        }

        @Override
        public String getRemoteEndpointName() {
            return wrapped.getRemoteEndpointName();
        }

        @Override
        public IoFuture<Channel> openChannel(String serviceType, OptionMap optionMap) {
            channelOpened = true;
            return wrapped.openChannel(serviceType, optionMap);
        }

        @Override
        public SSLSession getSslSession() {
            throw new IllegalStateException("No obtaining the Ssl session expected.");
        }

        @Override
        public ConnectionPeerIdentity getConnectionPeerIdentity() throws SecurityException {
            return wrapped.getConnectionPeerIdentity();
        }

        @Override
        public ConnectionPeerIdentity getConnectionAnonymousIdentity() {
            return wrapped.getConnectionAnonymousIdentity();
        }

        @Override
        public ConnectionPeerIdentityContext getPeerIdentityContext() {
            return wrapped.getPeerIdentityContext();
        }

        @Override
        public Principal getPrincipal() {
            return wrapped.getPrincipal();
        }


    }

    private class TestOpenListener implements OpenListener {

        private Connection connection;

        @Override
        public synchronized void channelOpened(Channel channel) {
            System.out.println("Channel Opened");
            connection = channel.getConnection();
            notifyAll();
        }

        private synchronized Connection getConnection() throws Exception {
            if (connection == null) {
                wait();
            }

            return connection;
        }

        @Override
        public void registrationTerminated() {
        }

    }

}
