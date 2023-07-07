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

import static org.jboss.remotingjmx.Constants.PROTOCOL_HTTPS_REMOTING_JMX;
import static org.jboss.remotingjmx.Constants.PROTOCOL_HTTP_REMOTING_JMX;
import static org.jboss.remotingjmx.Constants.PROTOCOL_REMOTE;
import static org.jboss.remotingjmx.Constants.PROTOCOL_REMOTE_HTTP;
import static org.jboss.remotingjmx.Constants.PROTOCOL_REMOTE_HTTPS;
import static org.jboss.remotingjmx.Constants.PROTOCOL_REMOTE_TLS;
import static org.jboss.remotingjmx.Constants.PROTOCOL_REMOTING_JMX;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;

import org.jboss.logging.Logger;

/**
 * The JMXConnectorProvider implementation for use with Remoting.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RemotingConnectorProvider implements JMXConnectorProvider {

    private static final Logger log = Logger.getLogger(RemotingConnectorProvider.class);

    @SuppressWarnings("deprecation")
    public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
        String protocol = serviceURL.getProtocol();
        switch (protocol) {
            case PROTOCOL_REMOTE:
            case PROTOCOL_REMOTE_TLS:
            case PROTOCOL_REMOTING_JMX:
            case PROTOCOL_REMOTE_HTTP:
            case PROTOCOL_HTTP_REMOTING_JMX:
            case PROTOCOL_REMOTE_HTTPS:
            case PROTOCOL_HTTPS_REMOTING_JMX:
                return new RemotingConnector(serviceURL, environment);
            default:
                log.tracef("Protocol (%s) not recognised by this provider.", protocol);

                return null;
        }
    }

    /**
     * Get the version string of the remoting connector provider.
     *
     * @return the version string.
     */
    public static String getVersionString() {
        return Version.getVersionString();
    }

}
