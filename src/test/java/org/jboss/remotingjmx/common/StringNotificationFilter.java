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

import javax.management.Notification;
import javax.management.NotificationFilter;

/**
 * A simple NotificationFilter for String notifications.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class StringNotificationFilter implements NotificationFilter {

    private static final long serialVersionUID = -3106287394464009625L;
    private String filter;

    public StringNotificationFilter(final String filter) {
        this.filter = filter;
    }

    public boolean isNotificationEnabled(Notification notification) {
        Object userData = notification.getUserData();
        if (userData instanceof String) {
            return ((String) userData).contains(filter);
        }

        return false;
    }

    public int hashCode() {
        int hash = StringNotificationFilter.class.getName().hashCode();
        return filter == null ? hash : hash + filter.hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof StringNotificationFilter) {
            StringNotificationFilter snf = (StringNotificationFilter) other;
            return filter == null ? snf.filter == null : filter.equals(snf.filter);
        }

        return false;
    }

}
