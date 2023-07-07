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
