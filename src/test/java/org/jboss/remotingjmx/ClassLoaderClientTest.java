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
import static org.junit.Assert.assertNotSame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.remotingjmx.common.CustomValueClass;
import org.jboss.remotingjmx.common.MyBean;
import org.junit.Test;

/**
 * Tests that class loader based serialization/deserialization works. An MBean is created in a custom class loader and installed
 * into the server.
 *
 * This MBean is then called with an instance of the parameter class coming the main class loader. If the incorrect class loader
 * is used for de-serialization then a ClassCastException will result when the method is invoked.
 *
 * @author Stuart Douglas
 */
public class ClassLoaderClientTest extends AbstractTestBase {

    @Test
    public void testInvoke() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testClassLoading");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        final Class<?> mbeanClass = TEST_CLASS_LOADER.loadClass(MyBean.class.getName());
        Object bean = mbeanClass.newInstance();

        // we make sure that the bean's parameter types are actually loaded by the
        // separate class loader

        Method method = null;
        for (final Method m : mbeanClass.getDeclaredMethods()) {
            if (m.getName().equals("extractCustomValue")) {
                method = m;
                break;
            }
        }

        assertNotSame(CustomValueClass.class, method.getParameterTypes()[0]);

        assertFalse(connection.isRegistered(beanName));
        mbeanServer.registerMBean(bean, beanName);
        try {
            final CustomValueClass someValue = new CustomValueClass("MyTestValue");

            String response = (String) connection.invoke(beanName, "extractCustomValue", new Object[] { someValue },
                    new String[] { CustomValueClass.class.getName() });

            assertNotNull(response);
            assertEquals("MyTestValue", response);

        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }

    }

    private static final ClassLoader TEST_CLASS_LOADER = new ClassLoader() {

        @Override
        protected Class<?> loadClass(final String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("org.jboss.remotingjmx")) {
                return getClass().getClassLoader().loadClass(name);
            }

            String path = name.replace('.', '/').concat(".class");
            final URL res = getClass().getClassLoader().getResource(path);

            if (res != null) {
                InputStream stream = null;
                try {
                    stream = res.openStream();

                    byte[] data = new byte[512];
                    int read = 0;
                    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    while ((read = stream.read(data)) != -1) {
                        bytes.write(data, 0, read);
                    }
                    try {
                        return defineClass(name, bytes.toByteArray(), 0, bytes.toByteArray().length);
                    } catch (Exception e) {
                        throw new ClassNotFoundException(name, e);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException ignore) {

                        }
                    }
                }
            } else {
                throw new ClassNotFoundException(name);
            }
        }
    };

}
