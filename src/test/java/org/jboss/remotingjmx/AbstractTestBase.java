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

import java.io.IOException;

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

    private static JMXRemotingServer remotingServer;
    protected static MBeanServer mbeanServer;
    protected static JMXServiceURL serviceURL;

    protected JMXConnector connector;

    @BeforeClass
    public static void setupServer() throws IOException {
        String bindAddress = System.getProperty(BIND_ADDRESS_PROPERTY, DEFAULT_BIND_ADDRESS);

        mbeanServer = MBeanServerFactory.createMBeanServer(DEFAULT_DOMAIN);

        JMXRemotingConfig config = new JMXRemotingConfig();
        config.mbeanServer = mbeanServer;
        config.host = bindAddress;

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
        connector = JMXConnectorFactory.connect(serviceURL);
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
