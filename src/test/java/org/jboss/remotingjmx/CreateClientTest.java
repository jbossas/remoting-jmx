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
