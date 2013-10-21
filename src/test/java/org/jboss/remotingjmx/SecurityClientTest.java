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
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslException;

import org.jboss.logging.Logger;
import org.jboss.remotingjmx.common.JMXRemotingServer;
import org.jboss.remotingjmx.common.JMXRemotingServer.JMXRemotingConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case to test the various supported SASL mechanisms.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityClientTest {

    private static final Logger log = Logger.getLogger(SecurityClientTest.class);

    private JMXRemotingConfig config;
    private JMXServiceURL serviceURL;

    @Before
    public void initialise() throws MalformedURLException {
        String bindAddress = System.getProperty(BIND_ADDRESS_PROPERTY, DEFAULT_BIND_ADDRESS);
        config = new JMXRemotingConfig();
        config.host = bindAddress;

        serviceURL = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT);
    }

    @After
    public void cleanUp() {
        config = null;
        serviceURL = null;
    }

    @Test
    public void testAnonymousAuthentication() throws Exception {
        log.info("testAnonymousAuthentication - Begin");
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
        log.info("testDigestAuthentication - Begin");
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
        log.info("testLocalAuthentication - Begin");
        config.saslMechanisms = Collections.singleton(JBOSS_LOCAL_USER);

        JMXRemotingServer remotingServer = new JMXRemotingServer(config);
        remotingServer.start();

        Map<String, Object> env = new HashMap<String, Object>(1);
        // This should not disable local auth.
        env.put(CallbackHandler.class.getName(), new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                throw new UnsupportedCallbackException(callbacks[0]);
            }
        });

        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);

        assertNotNull(connector.getConnectionId());

        connector.close();

        remotingServer.stop();
    }

    @Test(expected=SaslException.class)
    public void testLocalAuthenticationDisabled() throws Exception {
        log.info("testLocalAuthentication - Begin");
        config.saslMechanisms = Collections.singleton(JBOSS_LOCAL_USER);

        JMXRemotingServer remotingServer = new JMXRemotingServer(config);
        remotingServer.start();

        Map<String, Object> env = new HashMap<String, Object>(1);
        env.put("org.jboss.remoting-jmx.excluded-sasl-mechanisms", JBOSS_LOCAL_USER);

        try {
            JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);
        } finally {
            remotingServer.stop();
        }
    }

    @Test(expected=SaslException.class)
    public void testLocalAuthenticationAutoDisabled() throws Exception {
        log.info("testLocalAuthentication - Begin");
        config.saslMechanisms = Collections.singleton(JBOSS_LOCAL_USER);

        JMXRemotingServer remotingServer = new JMXRemotingServer(config);
        remotingServer.start();

        Map<String, Object> env = new HashMap<String, Object>(1);
        env.put(JMXConnector.CREDENTIALS, new String[] { "DigestUser", "DigestPassword" });

        try {
            JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);
        } finally {
            remotingServer.stop();
        }
    }

    @Test
    public void testPlainAuthentication() throws Exception {
        log.info("testPlainAuthentication - Begin");
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
