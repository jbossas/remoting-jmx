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

import static org.jboss.remotingjmx.Constants.EXCLUDED_VERSIONS;
import static org.jboss.remotingjmx.common.Constants.BIND_ADDRESS_PROPERTY;
import static org.jboss.remotingjmx.common.Constants.DEFAULT_BIND_ADDRESS;
import static org.jboss.remotingjmx.common.Constants.PROTOCOL;
import static org.jboss.remotingjmx.common.JMXRemotingServer.DEFAULT_PORT;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.common.JMXRemotingServer;
import org.jboss.remotingjmx.common.JMXRemotingServer.JMXRemotingConfig;
import org.jboss.remotingjmx.protocol.Versions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A simple test using the DelegatingServer - primarily this test verifies that parameters can be sent from the client to the
 * server for MBeanServer selection.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ServerMessageInterceptorTest {

    private static final String DEFAULT_DOMAIN = "org.jboss.remotingjmx";

    private static JMXRemotingServer remotingServer;
    private static MBeanServer mbeanServer;
    private static String bindAddress;
    private static TestMBeanServerLocator mbeanServerLocator;

    protected JMXConnector connector;

    @BeforeClass
    public static void setupServer() throws IOException {
        bindAddress = System.getProperty(BIND_ADDRESS_PROPERTY, DEFAULT_BIND_ADDRESS);

        mbeanServer = MBeanServerFactory.createMBeanServer(DEFAULT_DOMAIN);
        mbeanServerLocator = new TestMBeanServerLocator();

        JMXRemotingConfig config = new JMXRemotingConfig();
        config.mbeanServer = mbeanServer;
        config.host = bindAddress;
        config.mbeanServerLocator = mbeanServerLocator;
        config.serverMessageInterceptorFactory = new TestServerMessageInterceptorFactory();

        remotingServer = new JMXRemotingServer(config);
        remotingServer.start();
    }

    @AfterClass
    public static void tearDownServer() throws IOException {
        try {
            remotingServer.stop();
        } finally {
            remotingServer = null;
        }
    }

    @Before
    public void reset() throws IOException {
        mbeanServerLocator.reset();
    }

    /**
     * Test a version one connection which will mean default MBeanServer.
     */
    @Test
    public void testVersionOne() throws Exception {
        doTestForVersion((byte) 0x01);
    }

    /**
     * Test a version two connection which will mean default MBeanServer.
     */
    @Test
    public void testVersionTwo() throws Exception {
        doTestForVersion((byte) 0x02);
    }

    private void doTestForVersion(byte versionId) throws Exception {
        Versions versions = new Versions(Collections.EMPTY_MAP);
        Set<Byte> supportedVersions = versions.getSupportedVersions();
        StringBuilder sb = null;
        for (Byte toAdd : supportedVersions) {
            if (toAdd != versionId) {
                if (sb == null) {
                    sb = new StringBuilder();
                } else {
                    sb.append(",");
                }
                sb.append(toAdd);
            }
        }

        Map<String, Object> environment = new HashMap<String, Object>();
        environment.put(EXCLUDED_VERSIONS, sb.toString());

        JMXServiceURL serviceUrl = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT);
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl, environment);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        TestServerMessageInterceptor.clear();
        connection.getDefaultDomain();
        TestServerMessageInterceptor.check();

        connector.close();
    }

    private static class TestMBeanServerLocator implements MBeanServerLocator {

        private boolean getDefaultMBeanServerCalled = false;
        private Map<String, String> parameters = null;

        @Override
        public MBeanServerConnection getDefaultMBeanServer() {
            getDefaultMBeanServerCalled = true;
            return mbeanServer;
        }

        @Override
        public MBeanServerConnection getMBeanServer(Map<String, String> parameters) {
            this.parameters = parameters;
            return mbeanServer;
        }

        private void reset() {
            getDefaultMBeanServerCalled = false;
            parameters = null;
        }
    }

    private static class TestServerMessageInterceptorFactory implements ServerMessageInterceptorFactory {
        @Override
        public ServerMessageInterceptor create(Channel channel) {
            return new TestServerMessageInterceptor();
        }
    }

    private static class TestServerMessageInterceptor implements ServerMessageInterceptor {
        static final Object LOCK = new Object();
        static volatile boolean before;
        static volatile CountDownLatch after;
        static volatile Throwable error;

        @Override
        public void handleEvent(Event event) throws IOException {
            beforeEvent();
            try {
                event.run();
                afterEvent(null);
            } catch (Throwable t) {
                afterEvent(t);
                if (t instanceof IOException) {
                    throw (IOException) t;
                } else {
                    throw new IOException(t);
                }
            }
        }

        private void beforeEvent() {
            before = true;
        }

        private void afterEvent(Throwable thrown) {
            error = thrown;
            after.countDown();
        }

        static void clear() {
            before = false;
            after = new CountDownLatch(1);
            error = null;
        }

        static void check() throws Exception {
            Assert.assertTrue(before);
            after.await(10, TimeUnit.MILLISECONDS);
            Assert.assertNull(error);
        }
    };
}
