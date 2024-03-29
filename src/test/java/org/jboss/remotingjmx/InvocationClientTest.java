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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.jboss.remotingjmx.common.MyBean;
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

    @Test
    public void testInvoke_Timeout() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testInvoke_Timeout");
        assertFalse(mbeanServer.isRegistered(beanName));

        Map<String, Object> env = new HashMap<String, Object>();
        env.put("org.jboss.remoting-jmx.timeout", "1");
        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);
        MBeanServerConnection connection = connector.getMBeanServerConnection();

        MyBean bean = new MyBean() {
            @Override
            public String transpose(String message) {
                try {
                    System.out.println("Sleeping");
                    Thread.sleep(2000);
                    System.out.println("Awake");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return super.transpose(message);
            }

        };

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(bean, beanName);
        try {
            final String someValue = "MyTestValue";

            connection.invoke(beanName, "transpose", new Object[] { someValue }, new String[] { String.class.getName() });

            fail("Expected exception not thrown.");

        } catch (IOException ignored) {
            // We expect this to indicate a timeout.
            assertTrue(ignored.getMessage().contains("status=WAITING"));
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }

    }

}
