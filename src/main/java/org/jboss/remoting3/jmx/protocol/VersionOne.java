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
package org.jboss.remoting3.jmx.protocol;

import javax.management.MBeanServerConnection;
import javax.security.auth.Subject;
import org.jboss.remoting3.Channel;

import org.jboss.remoting3.jmx.VersionedConnection;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class VersionOne {

    private VersionOne() {
    }

    static byte getVersionIdentifier() {
        return 0x01;
    }

    static VersionedConnection getConnection(final Channel channel) {
        ClientConnection connection = new ClientConnection(channel);
        connection.start();

        return connection;
    }

}


class ClientConnection implements VersionedConnection {

    private final Channel channel;
    private String connectionId;

    ClientConnection(final Channel channel) {
        this.channel = channel;
    }

    void start() {


        // TODO - Don't return until the connectionId has been set - at that point message exchange can begin.
    }


    public String getConnectionId() {
        if (connectionId == null) {
            throw new IllegalStateException("Connection ID not set");
        }

        return connectionId;
    }

    public MBeanServerConnection getMBeanServerConnection(Subject subject) {
        return null;
    }

    public void close() {

    }
}
