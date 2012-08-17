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

import static org.jboss.remotingjmx.Constants.EXCLUDED_VERSIONS;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.MBeanServerManager;
import org.jboss.remotingjmx.VersionedConnection;
import org.jboss.remotingjmx.VersionedProxy;
import org.jboss.remotingjmx.protocol.v1.VersionOne;
import org.jboss.remotingjmx.protocol.v2.VersionTwo;

/**
 * Single access point to locate the supported versions.
 * <p/>
 * As the client and server are written in parallel this makes no distinction between clients and servers when listing the
 * supported versions.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class Versions {

    private static final Logger log = Logger.getLogger(Versions.class);

    private final Map<String, ?> environment;
    private final Set<Byte> supportedVersions;

    public Versions(final Map<String, ?> environment) {
        this.environment = environment;

        Set<Byte> supportedVersions = new HashSet<Byte>();
        supportedVersions.add(VersionOne.getVersionIdentifier());
        supportedVersions.add(VersionTwo.getVersionIdentifier());
        supportedVersions.removeAll(getExcludedVersions());

        this.supportedVersions = Collections.unmodifiableSet(supportedVersions);
    }

    private Set<Byte> getExcludedVersions() {
        Set<Byte> excluded = new HashSet<Byte>();
        Object list;
        if (environment != null && environment.containsKey(EXCLUDED_VERSIONS)
                && (list = environment.get(EXCLUDED_VERSIONS)) != null) {
            if (list instanceof String) {
                split((String) list, excluded);
            } else {
                log.warnf("Ignoring excluded versions list of type '%s'", list.getClass().getName());
            }
        }
        split(System.getProperty(EXCLUDED_VERSIONS, ""), excluded);

        return excluded;
    }

    private void split(final String from, final Set<Byte> to) {
        String[] values = from.split(",");
        for (String current : values) {
            try {
                String temp = current.trim();
                if (temp.length() > 0) {
                    to.add(Byte.valueOf(current.trim()));
                }
            } catch (NumberFormatException e) {
                log.warnf("Unrecognised version '%s' in list.", current);
            }
        }
    }

    public Set<Byte> getSupportedVersions() {
        return supportedVersions;
    }

    public VersionedConnection getVersionedConnection(final byte version, final Channel channel) throws IOException {
        if (supportedVersions.contains(version)) {
            if (version == VersionOne.getVersionIdentifier()) {
                return VersionOne.getConnection(channel, environment);
            } else if (version == VersionTwo.getVersionIdentifier()) {
                return VersionTwo.getConnection(channel, environment);
            }
        } else {
            log.warnf("An attempt has been made to select an unsupported version 0x0%d", version);
        }

        throw new IllegalArgumentException("Unsupported protocol version.");
    }

    public VersionedProxy getVersionedProxy(final byte version, final Channel channel, final MBeanServerManager serverManager,
            final Executor executor) throws IOException {
        if (supportedVersions.contains(version)) {
            if (version == VersionOne.getVersionIdentifier()) {
                return VersionOne.getProxy(channel, serverManager.getDefaultMBeanServer(), executor);
            } else if (version == VersionTwo.getVersionIdentifier()) {
                return VersionTwo.getProxy(channel, serverManager.getDefaultMBeanServer(), executor);
            }
        } else {
            log.warnf("An attempt has been made to select an unsupported version 0x0%d", version);
        }

        throw new IllegalArgumentException("Unsupported protocol version.");
    }

}
