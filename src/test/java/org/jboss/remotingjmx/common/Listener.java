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

package org.jboss.remotingjmx.common;

import java.util.HashSet;
import java.util.Set;

import javax.management.Notification;
import javax.management.NotificationListener;

/**
 * A simple NotificationListener used for testing.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class Listener implements NotificationListener {

    private final Set<Pair> notifications = new HashSet<Pair>();

    public synchronized void handleNotification(Notification notification, Object handback) {
        Pair p = new Pair();
        p.notification = notification;
        p.handback = handback;
        notifications.add(p);
        notifyAll();
    }

    public synchronized Set<Pair> getRecievedNotifications() {
        try {
            return new HashSet<Pair>(notifications);
        } finally {
            notifications.clear();
        }
    }

    public synchronized Set<Pair> getNotEmptyNotofications(long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (notifications.size() == 0 && System.currentTimeMillis() - start < timeout) {
            wait(10);
        }

        return getRecievedNotifications();
    }

    public static class Pair {
        public Notification notification;
        public Object handback;
    }

}
