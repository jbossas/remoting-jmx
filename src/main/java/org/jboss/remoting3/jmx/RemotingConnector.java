/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Map;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RemotingConnector implements JMXConnector {

    public void connect() throws IOException {
        System.out.println("connect()");
    }

    public void connect(Map<String, ?> env) throws IOException {

        StringBuffer sb = new StringBuffer("connect(");
        if (env != null) {
            for (String key : env.keySet()) {
                Object current = env.get(key);
                if (current instanceof String[]) {
                    String[] temp = (String[]) current;
                    StringBuffer sb2 = new StringBuffer();
                    sb2.append("[username=").append(temp[0]).append(",password=").append(temp[1]).append("]");
                    current = sb2;
                }

                sb.append("{").append(key).append(",").append(String.valueOf(current)).append("}");
            }
        } else {
            sb.append("null");
        }
        sb.append(")");
        System.out.println(sb.toString());
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection() throws IOException {
        System.out.println("getMBeanServerConnection()");
        return null;
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
        System.out.println("getMBeanServerConnection(Subject)");
        return null;
    }

    @Override
    public void close() throws IOException {
        System.out.println("close()");
    }

    @Override
    public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        System.out.println("addConnectionNotificationListener()");
    }

    @Override
    public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        System.out.println("removeConnectionNotificationListener()");
    }

    @Override
    public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback)
            throws ListenerNotFoundException {
        System.out.println("removeConnectionNotificationListener()");
    }

    @Override
    public String getConnectionId() throws IOException {
        System.out.println("getConnectionId()");
        return null;
    }

}
