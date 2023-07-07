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
