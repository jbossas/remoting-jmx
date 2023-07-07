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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

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
public class DelegatingServerTest {

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
        Versions versions = new Versions(Collections.EMPTY_MAP);
        Set<Byte> supportedVersions = versions.getSupportedVersions();
        StringBuilder sb = null;
        for (Byte toAdd : supportedVersions) {
            if (toAdd != 0x01) {
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
        String defaultDomain = connection.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Remote Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        assertTrue("Default MBeanServer Userd", mbeanServerLocator.getDefaultMBeanServerCalled);
        assertNull("No properties sent.", mbeanServerLocator.parameters);

        connector.close();
    }

    /**
     * Test a version one connection which will mean default MBeanServer.
     */
    @Test
    public void testVersionOne_WithParameters() throws Exception {
        Versions versions = new Versions(Collections.EMPTY_MAP);
        Set<Byte> supportedVersions = versions.getSupportedVersions();
        StringBuilder sb = null;
        for (Byte toAdd : supportedVersions) {
            if (toAdd != 0x01) {
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

        JMXServiceURL serviceUrl = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT, "/jmx?a=b");
        try {
            JMXConnector connector = JMXConnectorFactory.connect(serviceUrl, environment);
            fail("Expected exception not thrown.");
        } catch (IllegalStateException expected) {
        }
    }

    /**
     * Test using a default URL with no extras.
     */
    @Test
    public void testDefaultURL() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT);
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        String defaultDomain = connection.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Remote Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        assertEquals("No properties sent.", 0, mbeanServerLocator.parameters.size());

        connector.close();
    }

    /**
     * Test a URL that overrides the channel name but no parameters.
     */
    @Test
    public void testChannel() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT, "/jmx");
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        String defaultDomain = connection.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Remote Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        assertEquals("No properties sent.", 0, mbeanServerLocator.parameters.size());

        connector.close();
    }

    /**
     * Test a URL that overrides the channel name and includes one parameters.
     */
    @Test
    public void testChannelOneParameter() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT, "/jmx?a=b");
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        String defaultDomain = connection.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Remote Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        assertEquals("No properties sent.", 1, mbeanServerLocator.parameters.size());
        assertEquals("Parameter set", "b", mbeanServerLocator.parameters.get("a"));

        connector.close();
    }

    /**
     * Test a URL that overrides the channel name and includes two parameters.
     */
    @Test
    public void testChannelTwoParameters() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT, "/jmx?a=b,c=d");
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        String defaultDomain = connection.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Remote Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        assertEquals("No properties sent.", 2, mbeanServerLocator.parameters.size());
        assertEquals("Parameter set", "b", mbeanServerLocator.parameters.get("a"));
        assertEquals("Parameter set", "d", mbeanServerLocator.parameters.get("c"));

        connector.close();
    }

    /**
     * Test a URL with one parameter and no channel override.
     */
    @Test
    public void testOneParameter() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT, "/?a=b");
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        String defaultDomain = connection.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Remote Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        assertEquals("No properties sent.", 1, mbeanServerLocator.parameters.size());
        assertEquals("Parameter set", "b", mbeanServerLocator.parameters.get("a"));

        connector.close();
    }

    /**
     * Test a URL with two parameters and no channel override.
     */
    @Test
    public void testTwoParameters() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT, "/?a=b,c=d");
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        String defaultDomain = connection.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Remote Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        assertEquals("No properties sent.", 2, mbeanServerLocator.parameters.size());
        assertEquals("Parameter set", "b", mbeanServerLocator.parameters.get("a"));
        assertEquals("Parameter set", "d", mbeanServerLocator.parameters.get("c"));

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

}
