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

package org.jboss.remotingjmx.protocol;

import static org.jboss.remotingjmx.Constants.EXCLUDED_VERSIONS;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.management.remote.JMXServiceURL;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.Capability;
import org.jboss.remotingjmx.MBeanServerManager;
import org.jboss.remotingjmx.ServerMessageInterceptor;
import org.jboss.remotingjmx.VersionedConnection;
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
    private final Map<Byte, Set<Capability>> supportedVersions;

    public Versions(final Map<String, ?> environment) {
        this.environment = environment;

        Map<Byte, Set<Capability>> supportedVersions = new HashMap<Byte, Set<Capability>>();
        supportedVersions.put(VersionOne.getVersionIdentifier(), VersionOne.getCapabilites());
        supportedVersions.put(VersionTwo.getVersionIdentifier(), VersionTwo.getCapabilities());
        for (Byte current : getExcludedVersions()) {
            supportedVersions.remove(current);
        }

        this.supportedVersions = Collections.unmodifiableMap(supportedVersions);
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

    public Set<Byte> getSupportedVersions(Capability... capabilities) {
        if (capabilities.length > 0) {
            Set<Byte> filteredSupported = new HashSet<Byte>(supportedVersions.keySet());
            for (Byte current : supportedVersions.keySet()) {
                Set<Capability> currentCapabilities = supportedVersions.get(current);
                for (Capability toCheck : capabilities) {
                    if (currentCapabilities.contains(toCheck) == false) {
                        filteredSupported.remove(current);
                        continue;
                    }
                }
            }

            return filteredSupported;
        }
        return supportedVersions.keySet();
    }

    public VersionedConnection getVersionedConnection(final byte version, final Channel channel, final JMXServiceURL serviceURL)
            throws IOException {
        if (supportedVersions.containsKey(version)) {
            if (version == VersionOne.getVersionIdentifier()) {
                return VersionOne.getConnection(channel, environment);
            } else if (version == VersionTwo.getVersionIdentifier()) {
                return VersionTwo.getConnection(channel, environment, serviceURL);
            }
        } else {
            log.warnf("An attempt has been made to select an unsupported version 0x0%d", version);
        }

        throw new IllegalArgumentException("Unsupported protocol version.");
    }

    public void startServer(final byte version, final Channel channel, final MBeanServerManager serverManager,
            final Executor executor, final ServerMessageInterceptor serverMessageInterceptor) throws IOException {
        if (supportedVersions.containsKey(version)) {
            if (version == VersionOne.getVersionIdentifier()) {
                VersionOne.startServer(channel, serverManager.getDefaultMBeanServer(), executor, serverMessageInterceptor);
            } else if (version == VersionTwo.getVersionIdentifier()) {
                VersionTwo.startServer(channel, serverManager, executor, serverMessageInterceptor);
            }
            return;
        } else {
            log.warnf("An attempt has been made to select an unsupported version 0x0%d", version);
        }
        throw new IllegalArgumentException("Unsupported protocol version.");
    }
}
