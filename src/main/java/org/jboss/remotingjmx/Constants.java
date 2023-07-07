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

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class Constants {

    // Supported System and Environment Properties
    /**
     * Property used to specify the timeout within clients when waiting for a response from the server.
     *
     * Currently used within version 0x01 and 0x02 of the protocol, however this property may be unsupported in future protocol
     * versions when update message exchanges are added.
     */
    public static final String TIMEOUT_KEY = "org.jboss.remoting-jmx.timeout";
    /**
     * A comma separated list of protocol versions to be excluded from the list of supported versions.
     *
     * Server side this property causes the specified versions to be removed from the list of advertised versions and will also
     * prevent selection of the mechanism if selected despite not being offered.
     *
     * Client side this property will cause the specified mechanisms to be removed from the list of mechanisms supported by the
     * client, if the server offers the excluded mechanism the client will not select it.
     *
     * If the value is specified as both a System property and within the environment map then both will be combined, care
     * should be taken to avoid excluding all mechanisms.
     *
     * The System property is read on initialisation so should not be used to attempt real time updates to the list of supported
     * versions.
     */
    public static final String EXCLUDED_VERSIONS = "org.jboss.remoting-jmx.excluded-versions";

    /**
     * A comma separated list of SASL mechanisms that should be excluded when negotiating the connection to the server.
     */
    public static final String EXCLUDED_SASL_MECHANISMS = "org.jboss.remoting-jmx.excluded-sasl-mechanisms";


    static final String PROTOCOL_REMOTE = "remote";
    static final String PROTOCOL_REMOTE_TLS = "remote+tls";
    static final String PROTOCOL_REMOTE_HTTP = "remote+http";
    static final String PROTOCOL_REMOTE_HTTPS = "remote+https";

    @Deprecated
    static final String PROTOCOL_REMOTING_JMX = "remoting-jmx";
    @Deprecated
    static final String PROTOCOL_HTTP_REMOTING_JMX = "http-remoting-jmx";
    @Deprecated
    static final String PROTOCOL_HTTPS_REMOTING_JMX = "https-remoting-jmx";

    static final String REMOTE_SCHEME = "remote";
    static final String HTTP_SCHEME = "remote+http";
    static final String HTTPS_SCHEME = "remote+https";

    static final String CHANNEL_NAME = "jmx";

    static final byte STABLE = 0x00;
    static final byte SNAPSHOT = 0x01;

    static final String JMX = "JMX";
    static final byte[] JMX_BYTES = JMX.getBytes();

    static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";

}
