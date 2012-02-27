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
package org.jboss.remotingjmx.protocol;

import java.io.IOException;
import java.util.Map;

import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.RemotingConnectorServer;
import org.jboss.remotingjmx.VersionedConnection;
import org.jboss.remotingjmx.VersionedProxy;
import org.jboss.remotingjmx.protocol.v1.VersionOne;

/**
 * Single access point to locate the supported versions.
 * <p/>
 * As the client and server are written in parallel this makes no distinction between clients and servers when listing the
 * supported versions.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class Versions {

    /**
     * Private constructor, static methods will be used to locate the supported versions and instantiate them.
     */
    private Versions() {
    }

    public static byte[] getSupportedVersions() {
        // At a later point a more complex registry or discovery could be implemented.
        return new byte[] { VersionOne.getVersionIdentifier() };
    }

    public static VersionedConnection getVersionedConnection(final byte version, final Channel channel,
            final Map<String, ?> environment) throws IOException {
        if (version == VersionOne.getVersionIdentifier()) {
            return VersionOne.getConnection(channel, environment);
        }

        throw new IllegalArgumentException("Unsupported protocol version.");
    }

    public static VersionedProxy getVersionedProxy(final byte version, final Channel channel,
            final RemotingConnectorServer server) throws IOException {
        if (version == VersionOne.getVersionIdentifier()) {
            return VersionOne.getProxy(channel, server);
        }

        throw new IllegalArgumentException("Unsupported protocol version.");
    }

}
