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
import static org.junit.Assert.assertTrue;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.jboss.remotingjmx.common.MyBean;
import org.junit.Test;

/**
 * Test case to test calls to create.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class CreateClientTest extends AbstractTestBase {

    @Test
    public void testCreate() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testCreate");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        try {
            assertFalse(connection.isRegistered(beanName));
            ObjectInstance instance = connection.createMBean(MyBean.class.getName(), beanName);
            assertEquals(beanName, instance.getObjectName());
            assertEquals(MyBean.class.getName(), instance.getClassName());
            assertTrue(connection.isRegistered(beanName));
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }
    }

    @Test
    public void tesCreateWithArgs() throws Exception {
        ObjectName beanName = new ObjectName(DEFAULT_DOMAIN, "test", "testCreate");
        assertFalse(mbeanServer.isRegistered(beanName));

        MBeanServerConnection connection = connector.getMBeanServerConnection();

        try {
            assertFalse(connection.isRegistered(beanName));
            Object[] args = new String[] { "a", "b" };
            String[] types = new String[] { String.class.getName(), String.class.getName() };
            ObjectInstance instance = connection.createMBean(MyBean.class.getName(), beanName, args, types);
            assertEquals(beanName, instance.getObjectName());
            assertEquals(MyBean.class.getName(), instance.getClassName());
            assertTrue(connection.isRegistered(beanName));
        } finally {
            if (mbeanServer.isRegistered(beanName)) {
                mbeanServer.unregisterMBean(beanName);
            }
        }
    }
}
