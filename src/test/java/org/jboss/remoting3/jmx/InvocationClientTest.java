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

package org.jboss.remoting3.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.jboss.remoting3.jmx.common.MyBean;
import org.junit.Test;

/**
 * Test case to test interactions actually invoking an MBean
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class InvocationClientTest extends AbstractTestBase {

    @Test
    public void testGetAttribute() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testGetAttribute");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        MyBean bean = new MyBean();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(bean, beanName);
        try {
            final String someValue = "MyTestValue";
            bean.setSomeValue(someValue);

            assertEquals(someValue, connection.getAttribute(beanName, "SomeValue"));

            try {
                connection.getAttribute(beanName, "NoValue");
                fail("Expected exception not thrown");
            } catch (AttributeNotFoundException expected) {
            }
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }

    }

    @Test
    public void testGetAttributes() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testGetAttributes");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        MyBean bean = new MyBean();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(bean, beanName);
        try {
            final String someValue = "MyTestValue";
            final String anotherValue = "AnotherValue";
            bean.setSomeValue(someValue);
            bean.setAnotherValue(anotherValue);

            AttributeList al = connection.getAttributes(beanName, new String[] { "SomeValue", "AnotherValue" });
            assertNotNull(al);
            assertEquals(2, al.size());

            for (int i = 0; i < 2; i++) {
                Attribute attr = (Attribute) al.get(i);
                if ("SomeValue".equals(attr.getName())) {
                    assertEquals(someValue, attr.getValue());
                } else if ("AnotherValue".equals(attr.getName())) {
                    assertEquals(anotherValue, attr.getName());
                } else {
                    fail("Unexpected attribute '" + attr.getName() + "' returned.");
                }
            }
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }

    }

    @Test
    public void testSetAttribute() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testSetAttribute");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        MyBean bean = new MyBean();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(bean, beanName);
        try {
            final String someValue = "MyTestValue";

            bean.setSomeValue("");
            assertEquals("", connection.getAttribute(beanName, "SomeValue"));
            connection.setAttribute(beanName, new Attribute("SomeValue", someValue));
            assertEquals(someValue, connection.getAttribute(beanName, "SomeValue"));

            try {
                connection.setAttribute(beanName, new Attribute("NoValue", someValue));
                fail("Expected exception not thrown.");
            } catch (AttributeNotFoundException expected) {
            }
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }

    }

    @Test
    public void testSetAttributes() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testSetAttributes");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        MyBean bean = new MyBean();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(bean, beanName);
        try {
            final String someValue = "MyTestValue";

            bean.setSomeValue("");

            // Only one of these should actually be set.
            AttributeList attrs = new AttributeList();
            attrs.add(new Attribute("SomeValue", someValue));
            attrs.add(new Attribute("NoValue", "NONE"));

            assertEquals("", connection.getAttribute(beanName, "SomeValue"));
            AttributeList response = connection.setAttributes(beanName, attrs);
            assertNotNull(response);
            assertEquals(1, response.size());

            assertEquals("SomeValue", ((Attribute) response.get(0)).getName());
            assertEquals(someValue, ((Attribute) response.get(0)).getValue());

            assertEquals(someValue, connection.getAttribute(beanName, "SomeValue"));
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }

    }

    @Test
    public void testInvoke() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testInvoke");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        MyBean bean = new MyBean();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(bean, beanName);
        try {
            final String someValue = "MyTestValue";

            String response = (String) connection.invoke(beanName, "transpose", new Object[] { someValue },
                    new String[] { String.class.getName() });

            assertNotNull(response);
            assertEquals("eulaVtseTyM", response);

            try {
                connection.invoke(beanName, "noMethod", new Object[] { someValue }, new String[] { String.class.getName() });
                fail("Expected exception not thrown");
            } catch (ReflectionException expected) {
            }
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }

    }

    @Test
    public void testInvoke_NPE() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testInvoke_NPE");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        MyBean bean = new MyBean();

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(bean, beanName);
        try {
            connection.invoke(beanName, "transpose", new Object[] { null }, new String[] { String.class.getName() });
            fail("Expected exception not thrown.");
        } catch (RuntimeException e) {
            assertEquals(NullPointerException.class, e.getCause().getClass());
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }

    }

}
