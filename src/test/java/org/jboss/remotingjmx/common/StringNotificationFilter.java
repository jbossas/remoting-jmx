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
