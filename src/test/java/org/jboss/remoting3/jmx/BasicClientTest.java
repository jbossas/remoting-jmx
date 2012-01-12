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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.remoting3.jmx.common.MyBean;
import org.junit.Test;

/**
 * Test case to test the basic client side interactions, the more complex scenarios are split into their own test cases.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class BasicClientTest extends AbstractTestBase {

    @Test
    public void testNewJMXConnector() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(URL);
        JMXConnector connector = JMXConnectorFactory.newJMXConnector(serviceUrl, null);

        assertNotNull(connector);
    }

    @Test
    public void testConnect() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(URL);
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);

        assertNotNull(connector);
    }

    @Test
    public void testConnect_URI() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(URL + "/jmx");
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);

        assertNotNull(connector);
    }

    @Test
    public void testConnectId() throws Exception {
        String connectionId = connector.getConnectionId();
        assertNotNull("connectionId", connectionId);
        assertTrue("connectionId length", connectionId.length() > 0);
    }

    @Test
    public void testGetDefaultDomain() throws Exception {
        String defaultDomain = mbeanServer.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Direct Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        defaultDomain = connection.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Remote Access Default Domain", DEFAULT_DOMAIN, defaultDomain);
    }

    @Test
    public void testGetMBeanCount() throws Exception {
        Integer count = mbeanServer.getMBeanCount();
        assertNotNull("count", count);
        assertEquals("Direct Access MBeanCount", (Integer) 1, count);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        count = connection.getMBeanCount();
        assertNotNull("count", count);
        assertEquals("Remote Access MBeanCount", (Integer) 1, count);
    }

    @Test
    public void testGetDomains() throws Exception {
        String[] expectedDomains = mbeanServer.getDomains();
        assertNotNull("expectedDomains", expectedDomains);
        assertEquals("Expected Domains Length", 1, expectedDomains.length);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        String[] receivedDomains = connection.getDomains();

        assertNotNull("receivedDomains", receivedDomains);
        assertEquals("Received Domains Length", 1, receivedDomains.length);

        assertEquals(expectedDomains[0], receivedDomains[0]);
    }

    @Test
    public void testGetObjectInstance() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testGetObjectInstance");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(new MyBean(), beanName);
        try {
            ObjectInstance oi = connection.getObjectInstance(beanName);
            assertEquals(beanName, oi.getObjectName());

            connection.unregisterMBean(beanName);
            try {
                connection.getObjectInstance(beanName);
                fail("Excpected exception not thrown.");
            } catch (InstanceNotFoundException expected) {
            }

        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }
    }

    @Test
    public void testGetMBeanInfoInstance() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testGetMBeanInfoInstance");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(new MyBean(), beanName);
        try {
            MBeanInfo info = connection.getMBeanInfo(beanName);
            assertEquals(MyBean.class.getName(), info.getClassName());

            connection.unregisterMBean(beanName);
            try {
                connection.getMBeanInfo(beanName);
                fail("Excpected exception not thrown.");
            } catch (InstanceNotFoundException expected) {
            }

        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }
    }

    @Test
    public void testIsRegistered() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testIsRegistered");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(new MyBean(), beanName);
        try {
            assertTrue(connection.isRegistered(beanName));
        } finally {
            mbeanServer.unregisterMBean(beanName);
        }
    }

    @Test
    public void testIsInstanceOf() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testIsInstanceOf");

        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        try {
            connection.isInstanceOf(beanName, MyBean.class.getName());
            fail("Expected exception not thrown");
        } catch (InstanceNotFoundException expected) {
        }

        mbeanServer.registerMBean(new MyBean(), beanName);
        try {
            assertFalse(connection.isInstanceOf(beanName, String.class.getName()));
            assertTrue(connection.isInstanceOf(beanName, MyBean.class.getName()));
        } finally {
            mbeanServer.unregisterMBean(beanName);
        }
    }

    @Test
    public void testUnregisterMBean() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testUnregisterMBean");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(new MyBean(), beanName);
        try {
            assertTrue(connection.isRegistered(beanName));
            connection.unregisterMBean(beanName);
            assertFalse(connection.isRegistered(beanName));

            try {
                connection.unregisterMBean(beanName);
                fail("Excpected exception not thrown.");
            } catch (InstanceNotFoundException expected) {
            }

        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }
    }

    @Test
    public void testQueryMBeans() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testQueryMBeans");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(new MyBean(), beanName);
        try {
            Set<ObjectInstance> instances = connection.queryMBeans(null, null);

            boolean found = false;
            for (ObjectInstance current : instances) {
                if (beanName.equals(current.getObjectName())) {
                    found = true;
                    break;
                }
            }
            assertTrue("Found our bean", found);
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }
    }

    @Test
    public void testQueryNames() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testQueryNames");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(new MyBean(), beanName);
        try {
            Set<ObjectName> names = connection.queryNames(null, null);

            boolean found = false;
            for (ObjectName current : names) {
                if (beanName.equals(current)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Found our bean", found);
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }
    }
}
