/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.remoting3.jmx;

import static org.junit.Assert.assertNotNull;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.io.IOException;

import org.jboss.remoting3.jmx.common.JMXRemotingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case to test the client side of the interactions.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ClientTest {

    private static final int PORT = 12345;

    private static final String URL = "service:jmx:remoting://localhost:" + PORT;

    private static JMXRemotingServer remotingServer;

    @BeforeClass
    public static void setupServer() throws IOException {
        remotingServer = new JMXRemotingServer(PORT);
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

    @Test
    public void testNewJMXConnector() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(URL);
        JMXConnector connector = JMXConnectorFactory.newJMXConnector(serviceUrl, null);

        assertNotNull(connector);
    }

}
