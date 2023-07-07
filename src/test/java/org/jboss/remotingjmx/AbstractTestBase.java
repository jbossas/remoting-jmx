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
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.remotingjmx.common.JMXRemotingServer;
import org.jboss.remotingjmx.common.JMXRemotingServer.JMXRemotingConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * The base class for the remote tests.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class AbstractTestBase {

    protected static final String DEFAULT_DOMAIN = "org.jboss.remotingjmx";

    protected static JMXRemotingServer remotingServer;
    protected static MBeanServer mbeanServer;
    protected static JMXServiceURL serviceURL;
    protected static String bindAddress;

    protected JMXConnector connector;

    @BeforeClass
    public static void setupServer() throws IOException {
        setupServer(null);
    }

    public static void setupServer(String excludedVersions) throws IOException {
        bindAddress = System.getProperty(BIND_ADDRESS_PROPERTY, DEFAULT_BIND_ADDRESS);

        mbeanServer = MBeanServerFactory.createMBeanServer(DEFAULT_DOMAIN);

        JMXRemotingConfig config = new JMXRemotingConfig();
        config.mbeanServer = mbeanServer;
        config.host = bindAddress;
        config.excludedVersions = excludedVersions;

        remotingServer = new JMXRemotingServer(config);
        remotingServer.start();
        serviceURL = new JMXServiceURL(PROTOCOL, bindAddress, DEFAULT_PORT);
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
    public void connect() throws IOException {
        connect(null);
    }

    public void connect(String excludedVersions) throws IOException {
        Map<String, Object> environment = new HashMap<String, Object>();
        if (excludedVersions != null) {
            environment.put(EXCLUDED_VERSIONS, excludedVersions);
        }
        connector = JMXConnectorFactory.connect(serviceURL, environment);
    }

    @After
    public void disconnect() throws IOException {
        try {
            if (connector != null) {
                connector.close();
            }
        } finally {
            connector = null;
        }
    }

}
