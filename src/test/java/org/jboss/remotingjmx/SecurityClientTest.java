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

import static org.jboss.remotingjmx.common.Constants.BIND_ADDRESS_PROPERTY;
import static org.jboss.remotingjmx.common.Constants.DEFAULT_BIND_ADDRESS;
import static org.jboss.remotingjmx.common.Constants.PROTOCOL;
import static org.jboss.remotingjmx.common.JMXRemotingServer.DEFAULT_PORT;
import static org.jboss.remotingjmx.common.JMXRemotingServer.DIGEST_MD5;
import static org.jboss.remotingjmx.common.JMXRemotingServer.JBOSS_LOCAL_USER;
import static org.jboss.remotingjmx.common.JMXRemotingServer.PLAIN;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.remotingjmx.common.JMXRemotingServer;
import org.jboss.remotingjmx.common.JMXRemotingServer.JMXRemotingConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case to test the various supported SASL mechanisms.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityClientTest {

    private static JMXRemotingConfig config;
    private static JMXServiceURL serviceURL;

    @BeforeClass
    public static void initialise() throws MalformedURLException {
        String bindAddress = System.getProperty(BIND_ADDRESS_PROPERTY, DEFAULT_BIND_ADDRESS);
        config = new JMXRemotingConfig();
        config.host = bindAddress;

        serviceURL = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT);
    }

    @AfterClass
    public static void cleanUp() {
        config = null;
        serviceURL = null;
    }

    @Test
    public void testAnonymousAuthentication() throws Exception {
        JMXRemotingServer remotingServer = new JMXRemotingServer(config);
        remotingServer.start();

        Map<String, Object> env = new HashMap<String, Object>(0);

        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);

        assertNotNull(connector.getConnectionId());

        connector.close();

        remotingServer.stop();

        System.out.println("STOPPED");
    }

    @Test
    public void testDigestAuthentication() throws Exception {
        config.saslMechanisms = Collections.singleton(DIGEST_MD5);

        JMXRemotingServer remotingServer = new JMXRemotingServer(config);
        remotingServer.start();

        Map<String, Object> env = new HashMap<String, Object>(1);
        env.put(JMXConnector.CREDENTIALS, new String[] { "DigestUser", "DigestPassword" });

        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);

        assertNotNull(connector.getConnectionId());

        connector.close();

        // Now Try A Bad Password
        env.put(JMXConnector.CREDENTIALS, new String[] { "DigestUser", "BadPassword" });
        try {
            JMXConnectorFactory.connect(serviceURL, env);
            fail("Expected exception not thrown.");
        } catch (IOException expected) {
        }

        remotingServer.stop();
    }

    @Test
    public void testLocalAuthentication() throws Exception {
        config.saslMechanisms = Collections.singleton(JBOSS_LOCAL_USER);

        JMXRemotingServer remotingServer = new JMXRemotingServer(config);
        remotingServer.start();

        Map<String, Object> env = new HashMap<String, Object>(1);

        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);

        assertNotNull(connector.getConnectionId());

        connector.close();

        remotingServer.stop();
    }

    @Test
    public void testPlainAuthentication() throws Exception {
        config.saslMechanisms = Collections.singleton(PLAIN);

        JMXRemotingServer remotingServer = new JMXRemotingServer(config);
        remotingServer.start();

        Map<String, Object> env = new HashMap<String, Object>(1);
        env.put(JMXConnector.CREDENTIALS, new String[] { "DigestUser", "DigestPassword" });

        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);

        assertNotNull(connector.getConnectionId());

        connector.close();

        // Now Try A Bad Password
        env.put(JMXConnector.CREDENTIALS, new String[] { "DigestUser", "BadPassword" });
        try {
            JMXConnectorFactory.connect(serviceURL, env);
            fail("Expected exception not thrown.");
        } catch (IOException expected) {
        }

        remotingServer.stop();
    }

}
