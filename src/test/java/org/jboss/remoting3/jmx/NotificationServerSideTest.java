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

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.jboss.remoting3.jmx.common.ListenerBean;
import org.jboss.remoting3.jmx.common.NotificationBean;
import org.jboss.remoting3.jmx.common.StringNotificationFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case to test the server side notifications.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NotificationServerSideTest extends AbstractTestBase {

    private ObjectName notificationName;
    private ObjectName listenerName;

    private NotificationBean notificationBean;
    private ListenerBean listenerBean;

    private MBeanServerConnection connection;

    @Before
    public void register() throws Exception {
        notificationName = new ObjectName(DEFAULT_DOMAIN, "test", "notification");
        listenerName = new ObjectName(DEFAULT_DOMAIN, "test", "listener");

        notificationBean = new NotificationBean();
        mbeanServer.registerMBean(notificationBean, notificationName);
        listenerBean = new ListenerBean();
        mbeanServer.registerMBean(listenerBean, listenerName);
        connection = connector.getMBeanServerConnection();
    }

    @After
    public void unregister() throws Exception {
        if (mbeanServer.isRegistered(notificationName)) {
            mbeanServer.unregisterMBean(notificationName);
        }
        if (mbeanServer.isRegistered(listenerName)) {
            mbeanServer.unregisterMBean(listenerName);
        }
        connection = null;
    }

    @Test
    public void testInvoke() throws Exception {
        String theMessage = "Notification Message";
        connection.invoke(notificationName, "notify", new Object[] { theMessage }, new String[] { String.class.getName() });
    }

    @Test
    public void testAllNotifications() throws Exception {
        assertEquals(0, listenerBean.getRecievedNotifications().size());

        connection.addNotificationListener(notificationName, listenerName, null, null);

        String theMessage = "Notification Message 2";
        connection.invoke(notificationName, "notify", new Object[] { theMessage }, new String[] { String.class.getName() });

        Set<Notification> notifications = listenerBean.getRecievedNotifications();
        assertEquals(1, notifications.size());
        assertEquals(theMessage, notifications.iterator().next().getUserData());

        connection.removeNotificationListener(notificationName, listenerName);
        connection.invoke(notificationName, "notify", new Object[] { theMessage }, new String[] { String.class.getName() });
        assertEquals(0, listenerBean.getRecievedNotifications().size());
    }

    @Test
    public void testFilteredNotifications() throws Exception {
        assertEquals(0, listenerBean.getRecievedNotifications().size());

        NotificationFilter filter = new StringNotificationFilter("Keep");

        connection.addNotificationListener(notificationName, listenerName, filter, null);

        String keeper = "Notification 'Keep' Message";
        String thrower = "Notification 'Throw' Message";

        connection.invoke(notificationName, "notify", new Object[] { thrower }, new String[] { String.class.getName() });
        connection.invoke(notificationName, "notify", new Object[] { keeper }, new String[] { String.class.getName() });

        Set<Notification> notifications = listenerBean.getRecievedNotifications();
        assertEquals(1, notifications.size());
        assertEquals(keeper, notifications.iterator().next().getUserData());

        connection.removeNotificationListener(notificationName, listenerName);
        connection.invoke(notificationName, "notify", new Object[] { thrower }, new String[] { String.class.getName() });
        connection.invoke(notificationName, "notify", new Object[] { keeper }, new String[] { String.class.getName() });
        assertEquals(0, listenerBean.getRecievedNotifications().size());
    }

}
