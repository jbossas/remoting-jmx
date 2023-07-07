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

import static org.jboss.remotingjmx.common.Constants.BIND_ADDRESS_PROPERTY;
import static org.jboss.remotingjmx.common.Constants.DEFAULT_BIND_ADDRESS;
import static org.jboss.remotingjmx.common.Constants.PROTOCOL;
import static org.jboss.remotingjmx.common.JMXRemotingServer.DEFAULT_PORT;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.remoting3.Endpoint;
import org.jboss.remotingjmx.common.JMXRemotingServer;
import org.jboss.remotingjmx.common.JMXRemotingServer.JMXRemotingConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * A test case to verify the proxying capabilities of Remoting JMX with the MBeanServerLocator.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ProxyConnectionTest {

    protected static final String DOMAIN_ONE = "org.jboss.remotingjmx.domain_one";
    protected static final String DOMAIN_TWO = "org.jboss.remotingjmx.domain_two";
    protected static final String DOMAIN_THREE = "org.jboss.remotingjmx.domain_three";

    // The MBeanServers
    protected static MBeanServer domainOneMbeanServer;
    protected static MBeanServer domainTwoMbeanServer;
    protected static MBeanServer domainThreeMbeanServer;

    protected static JMXRemotingServer domainOneServer;
    protected static JMXRemotingServer domainTwoServer;

    protected static JMXServiceURL domainOneDirectUrl;
    protected static JMXServiceURL domainTwoDirectUrl;
    protected static JMXServiceURL domainTwoDirectByDefaultUrl;
    protected static JMXServiceURL domainThreeDirectUrl;
    protected static JMXServiceURL domainOneProxiedUrl;

    protected static String bindAddress;

    /**
     * First set up a JMXConnectorServer based server using the default port offset by one - this will be using domain_one.
     *
     * Then set up a DelegatingRemotingConnectorServer using the default server this time supplying an MBeanServerLocator that
     * uses a connection to the first server.
     *
     * The test MBeanServerLocator supports a key of domain= where the value is one of domain_one, domain_two, domain_three -
     * the connection will be established to the correct MBeanServer as appropriate. If no domain= key value is supplied then
     * the connection will be to domain_two by default.
     *
     * @throws IOException
     */
    @BeforeClass
    public static void setupServer() throws IOException {
        bindAddress = System.getProperty(BIND_ADDRESS_PROPERTY, DEFAULT_BIND_ADDRESS);

        domainOneMbeanServer = MBeanServerFactory.createMBeanServer(DOMAIN_ONE);
        domainTwoMbeanServer = MBeanServerFactory.createMBeanServer(DOMAIN_TWO);
        domainThreeMbeanServer = MBeanServerFactory.createMBeanServer(DOMAIN_THREE);

        /*
         * Set up the server for domain_one
         */
        JMXRemotingConfig config = new JMXRemotingConfig();
        config.endpoint = Endpoint.builder().build();
        config.mbeanServer = domainOneMbeanServer;
        config.host = bindAddress;
        config.port = DEFAULT_PORT + 1;

        domainOneServer = new JMXRemotingServer(config);
        domainOneServer.start();

        domainOneDirectUrl = new JMXServiceURL(PROTOCOL, bindAddress, config.port);

        /*
         * Set up the server for domain_two
         */
        config = new JMXRemotingConfig();
        config.endpoint = Endpoint.builder().build();
        config.host = bindAddress;
        config.port = DEFAULT_PORT;
        config.mbeanServerLocator = new TestMBeanServerLocator();

        domainTwoServer = new JMXRemotingServer(config);
        domainTwoServer.start();

        domainTwoDirectUrl = new JMXServiceURL(PROTOCOL, bindAddress, config.port, "/?domain=domain_two");
        domainTwoDirectByDefaultUrl = new JMXServiceURL(PROTOCOL, bindAddress, config.port);
        domainThreeDirectUrl = new JMXServiceURL(PROTOCOL, bindAddress, config.port, "/?domain=domain_three");
        domainOneProxiedUrl = new JMXServiceURL(PROTOCOL, bindAddress, config.port, "/?domain=domain_one");
    }

    @AfterClass
    public static void tearDownServer() throws IOException {
        // No fancy safe stuff - just cause the test to fail if we have a problem.
        domainOneServer.stop();
        domainOneServer = null;
        domainTwoServer.stop();
        domainTwoServer = null;
    }

    @Test
    public void testDomainOneDirect() throws Exception {
        JMXConnector connector = JMXConnectorFactory.connect(domainOneDirectUrl);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        assertEquals("Expected domain name", DOMAIN_ONE, connection.getDefaultDomain());

        connector.close();
    }

    @Test
    public void testDomainTwoDirect() throws Exception {
        JMXConnector connector = JMXConnectorFactory.connect(domainTwoDirectUrl);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        assertEquals("Expected domain name", DOMAIN_TWO, connection.getDefaultDomain());

        connector.close();
    }

    @Test
    public void testDomainTwoDirectByDefault() throws Exception {
        JMXConnector connector = JMXConnectorFactory.connect(domainTwoDirectByDefaultUrl);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        assertEquals("Expected domain name", DOMAIN_TWO, connection.getDefaultDomain());

        connector.close();
    }

    @Test
    public void testDomainThreeDirect() throws Exception {
        JMXConnector connector = JMXConnectorFactory.connect(domainThreeDirectUrl);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        assertEquals("Expected domain name", DOMAIN_THREE, connection.getDefaultDomain());

        connector.close();
    }

    @Test
    public void testDomainOneProxied() throws Exception {
        JMXConnector connector = JMXConnectorFactory.connect(domainOneProxiedUrl);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        assertEquals("Expected domain name", DOMAIN_ONE, connection.getDefaultDomain());

        connector.close();
    }

    private static class TestMBeanServerLocator implements MBeanServerLocator {

        @Override
        public MBeanServerConnection getDefaultMBeanServer() {
            return domainTwoMbeanServer;
        }

        @Override
        public MBeanServerConnection getMBeanServer(Map<String, String> parameters) {
            if (parameters.containsKey("domain")) {
                final String name = parameters.get("domain");
                if ("domain_one".equals(name)) {
                    try {
                        JMXConnector domainOneConnector = JMXConnectorFactory.connect(domainOneDirectUrl);

                        return domainOneConnector.getMBeanServerConnection();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if ("domain_two".equals(name)) {
                    return domainTwoMbeanServer;
                } else if ("domain_three".equals(name)) {
                    return domainThreeMbeanServer;
                } else {
                    return null;
                }
            }
            return domainTwoMbeanServer;
        }

    }

}
