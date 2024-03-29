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
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.remotingjmx.common.MyBean;
import org.junit.Test;

/**
 * Test case to test the basic client side interactions, the more complex scenarios are split into their own test cases.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class BasicClientTest extends AbstractTestBase {

    @Test
    public void testNewJMXConnector() throws Exception {
        JMXConnector connector = JMXConnectorFactory.newJMXConnector(serviceURL, null);

        assertNotNull(connector);
    }

    @Test
    public void testConnect() throws Exception {
        JMXConnector connector = JMXConnectorFactory.connect(serviceURL);

        assertNotNull(connector);
    }

    @Test
    public void testConnect_URI() throws Exception {
        JMXServiceURL serviceUrl = new JMXServiceURL(serviceURL.toString() + "/jmx");
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

    @Test
    public void testUnderlyingRemotingConnection() throws Exception {
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        RemotingMBeanServerConnection rmsc = (RemotingMBeanServerConnection) connection;
        assertNotNull("The underlying connection.", rmsc.getConnection());
    }

    @Test
    public void testInvokeForMBeanInBootstrapClassLoader() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testQueryNames");
        assertFalse(mbeanServer.isRegistered(beanName));

        System.out.println(StringBuffer.class.getClassLoader());

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        ModelMBeanInfoSupport info = new ModelMBeanInfoSupport(StringBuffer.class.getName(), "-",
                new ModelMBeanAttributeInfo[0],
                new ModelMBeanConstructorInfo[] {
                    new ModelMBeanConstructorInfo("-", StringBuffer.class.getConstructor())
                },
                new ModelMBeanOperationInfo[]{
                        new ModelMBeanOperationInfo("-", StringBuffer.class.getMethod("append", String.class))
                },
                new ModelMBeanNotificationInfo[0]);
        RequiredModelMBean mbean = new RequiredModelMBean(info);
        mbean.setManagedResource(new StringBuffer(), "ObjectReference");
        mbeanServer.registerMBean(mbean, beanName);
        try {
            System.out.println(mbeanServer.queryMBeans(null, null));
            connection.invoke(beanName, "append", new Object[]{"Test"}, new String[]{String.class.getName()});
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }
    }
}
