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

import junit.framework.Assert;

import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.common.JMXRemotingServer;
import org.jboss.remotingjmx.common.JMXRemotingServer.JMXRemotingConfig;
import org.jboss.remotingjmx.protocol.Versions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A simple test using the DelegatingServer - primarily this test verifies that parameters can be sent from the client to the
 * server for MBeanServer selection.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ServerMessageEventHandlerTest {

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
        config.serverMessageEventHandlerFactory = new TestServerMessageEventHandlerFactory();

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
        TestServerMessageEventHandler.clear();
        connection.getDefaultDomain();
        TestServerMessageEventHandler.check();

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

    private static class TestServerMessageEventHandlerFactory implements ServerMessageEventHandlerFactory {
        @Override
        public ServerMessageEventHandler create(Channel channel) {
            return new TestServerMessageEventHandler(channel);
        }
    }

    private static class TestServerMessageEventHandler extends ServerMessageEventHandler {
        static final Object LOCK = new Object();
        static volatile boolean before;
        static volatile CountDownLatch after;
        static volatile Throwable error;

        protected TestServerMessageEventHandler(Channel channel) {
            super(channel);
        }

        @Override
        public void beforeEvent() {
            before = true;
        }

        @Override
        public void afterEvent(Throwable thrown) {
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
