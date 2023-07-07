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
import static org.junit.Assert.assertNull;

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.remotingjmx.common.Listener;
import org.jboss.remotingjmx.common.Listener.Pair;
import org.jboss.remotingjmx.common.NotificationBean;
import org.jboss.remotingjmx.common.StringNotificationFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case to test the client side handling of notifications and the registrations.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NotificationClientSideTest extends AbstractTestBase {

    private ObjectName notificationName;
    private NotificationBean notificationBean;
    private Listener listener;

    private MBeanServerConnection connection;

    @Before
    public void register() throws Exception {
        notificationName = new ObjectName(DEFAULT_DOMAIN, "test", "notification");
        notificationBean = new NotificationBean() {

            @Override
            public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
                assertNull(handback);
                super.addNotificationListener(listener, filter, handback);
            }

        };
        mbeanServer.registerMBean(notificationBean, notificationName);
        connection = connector.getMBeanServerConnection();
        listener = new Listener();
    }

    @After
    public void unregister() throws Exception {
        if (mbeanServer.isRegistered(notificationName)) {
            mbeanServer.unregisterMBean(notificationName);
        }
        connection = null;
        listener = null;
    }

    @Test
    public void testInvoke() throws Exception {
        String theMessage = "Notification Message";
        connection.invoke(notificationName, "notify", new Object[] { theMessage }, new String[] { String.class.getName() });
    }

    @Test
    public void testAllNotifications() throws Exception {
        assertEquals(0, listener.getRecievedNotifications().size());

        String handback = "HANDBACK";
        connection.addNotificationListener(notificationName, listener, null, handback);

        String theMessage = "Notification Message 3";
        connection.invoke(notificationName, "notify", new Object[] { theMessage }, new String[] { String.class.getName() });

        Set<Pair> notifications = listener.getNotEmptyNotofications(2000);
        assertEquals(1, notifications.size());
        Pair p = notifications.iterator().next();
        assertEquals(theMessage, p.notification.getUserData());
        assertEquals(handback, p.handback);

        connection.removeNotificationListener(notificationName, listener);
        connection.invoke(notificationName, "notify", new Object[] { theMessage }, new String[] { String.class.getName() });
        assertEquals(0, listener.getRecievedNotifications().size());
    }

    @Test
    public void testFilteredNotifications() throws Exception {
        assertEquals(0, listener.getRecievedNotifications().size());

        NotificationFilter filter = new StringNotificationFilter("Keep");

        connection.addNotificationListener(notificationName, listener, filter, null);

        String keeper = "Notification 'Keep' Message";
        String thrower = "Notification 'Throw' Message";

        connection.invoke(notificationName, "notify", new Object[] { thrower }, new String[] { String.class.getName() });
        connection.invoke(notificationName, "notify", new Object[] { keeper }, new String[] { String.class.getName() });

        Set<Pair> notifications = listener.getNotEmptyNotofications(2000);
        assertEquals(1, notifications.size());
        assertEquals(keeper, notifications.iterator().next().notification.getUserData());

        connection.removeNotificationListener(notificationName, listener);
        connection.invoke(notificationName, "notify", new Object[] { thrower }, new String[] { String.class.getName() });
        connection.invoke(notificationName, "notify", new Object[] { keeper }, new String[] { String.class.getName() });
        assertEquals(0, listener.getRecievedNotifications().size());
    }

}
