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

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.jboss.remotingjmx.common.Listener.Pair;
import org.jboss.remotingjmx.common.ListenerBean;
import org.jboss.remotingjmx.common.NotificationBean;
import org.jboss.remotingjmx.common.StringNotificationFilter;
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

        Set<Pair> notifications = listenerBean.getRecievedNotifications();
        assertEquals(1, notifications.size());
        assertEquals(theMessage, notifications.iterator().next().notification.getUserData());

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

        Set<Pair> notifications = listenerBean.getRecievedNotifications();
        assertEquals(1, notifications.size());
        assertEquals(keeper, notifications.iterator().next().notification.getUserData());

        connection.removeNotificationListener(notificationName, listenerName);
        connection.invoke(notificationName, "notify", new Object[] { thrower }, new String[] { String.class.getName() });
        connection.invoke(notificationName, "notify", new Object[] { keeper }, new String[] { String.class.getName() });
        assertEquals(0, listenerBean.getRecievedNotifications().size());
    }

}
