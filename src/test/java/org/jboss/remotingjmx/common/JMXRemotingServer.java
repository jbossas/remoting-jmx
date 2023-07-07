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

import static org.jboss.remotingjmx.Constants.EXCLUDED_VERSIONS;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Provider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.security.sasl.SaslServerFactory;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.jboss.remotingjmx.DelegatingRemotingConnectorServer;
import org.jboss.remotingjmx.MBeanServerLocator;
import org.jboss.remotingjmx.RemotingConnectorServer;
import org.jboss.remotingjmx.ServerMessageInterceptorFactory;
import org.jboss.remotingjmx.Version;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm;
import org.wildfly.security.auth.realm.SimpleRealmEntry;
import org.wildfly.security.auth.server.MechanismConfiguration;
import org.wildfly.security.auth.server.MechanismRealmConfiguration;
import org.wildfly.security.auth.server.SaslAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.permission.PermissionVerifier;
import org.wildfly.security.sasl.util.FilterMechanismSaslServerFactory;
import org.wildfly.security.sasl.util.PropertiesSaslServerFactory;
import org.wildfly.security.sasl.util.SecurityProviderSaslServerFactory;
import org.wildfly.security.sasl.util.SortedMechanismSaslServerFactory;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * A test server to test exposing the local MBeanServer using Remoting.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class JMXRemotingServer {

    private static final WildFlyElytronProvider WILDFLY_ELYTRON_PROVIDER = new WildFlyElytronProvider();

    public static final String ANONYMOUS = "ANONYMOUS";
    public static final String DIGEST_MD5 = "DIGEST-MD5";
    public static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";
    public static final String PLAIN = "PLAIN";

    private static final String REALM = "JMX_Test_Realm";

    static final String REALM_PROPERTY = "com.sun.security.sasl.digest.realm";
    static final String PRE_DIGESTED_PROPERTY = "org.jboss.sasl.digest.pre_digested";
    static final String LOCAL_DEFAULT_USER = "jboss.sasl.local-user.default-user";
    static final String LOCAL_USER_CHALLENGE_PATH = "jboss.sasl.local-user.challenge-path";

    public static final int DEFAULT_PORT = 12345;
    private static final String PORT_PREFIX = "--port=";
    private static final String SASL_MECHANISM_PREFIX = "--sasl-mechanism=";

    private static final Logger log = Logger.getLogger(JMXRemotingServer.class);

    private final String host;
    private final int listenerPort;
    private final MBeanServer mbeanServer;
    private final Set<String> saslMechanisms;
    private final String excludedVersions;
    private final MBeanServerLocator mbeanServerLocator;
    private final ServerMessageInterceptorFactory serverMessageInterceptorFactory;
    private final SecurityDomain securityDomain;

    private Endpoint endpoint;
    private boolean closeEndpoint;
    private Closeable server;

    private JMXConnectorServer connectorServer;
    private DelegatingRemotingConnectorServer delegatingServer;

    /**
     * Constructor to instantiate a JMXRemotingServer with the default settings.
     *
     * @param port
     */
    public JMXRemotingServer() {
        this(new JMXRemotingConfig());
    }

    public JMXRemotingServer(JMXRemotingConfig config) {
        listenerPort = config.port > 0 ? config.port : DEFAULT_PORT;
        config.port = listenerPort; // Allow to be passed back to caller;
        host = config.host;
        mbeanServer = config.mbeanServer != null ? config.mbeanServer : ManagementFactory.getPlatformMBeanServer();
        saslMechanisms = Collections.unmodifiableSet(config.saslMechanisms != null ? config.saslMechanisms
                : Collections.emptySet());
        if (config.securityDomain != null) {
            securityDomain = config.securityDomain;
        } else {
            final SecurityDomain.Builder builder = SecurityDomain.builder();
            builder.setPermissionMapper((permissionMappable, roles) -> PermissionVerifier.ALL);
            final SimpleMapBackedSecurityRealm realm = new SimpleMapBackedSecurityRealm(() -> new Provider[] {WILDFLY_ELYTRON_PROVIDER});
            final Map<String, SimpleRealmEntry> passwordMap = new HashMap<>();
            passwordMap.put("$local", null);
            Password digestUserPassword = ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, "DigestPassword".toCharArray());
            passwordMap.put("DigestUser", new SimpleRealmEntry(Collections.singletonList(new PasswordCredential(digestUserPassword))));
            realm.setPasswordMap(passwordMap);
            builder.addRealm("default", realm).build();
            builder.setDefaultRealmName("default");
            securityDomain = builder.build();
        }
        excludedVersions = config.excludedVersions;
        mbeanServerLocator = config.mbeanServerLocator;
        this.serverMessageInterceptorFactory = config.serverMessageInterceptorFactory;
        if (config.endpoint != null) {
            endpoint = config.endpoint;
            closeEndpoint = true;
        }
    }

    public void start() throws IOException {
        log.infof("Starting JMX Remoting Server %s", Version.getVersionString());

        // Initialise general Remoting - this step would be implemented elsewhere when
        // running within an application server.
        if (endpoint == null) {
            endpoint = Endpoint.getCurrent();
        }

        final NetworkServerProvider nsp = endpoint.getConnectionProviderInterface("remote", NetworkServerProvider.class);
        final SocketAddress bindAddress = new InetSocketAddress(host, listenerPort);
        final OptionMap serverOptions = OptionMap.create(Options.SSL_ENABLED, false);

        SaslServerFactory saslServerFactory = new PropertiesSaslServerFactory(new SecurityProviderSaslServerFactory(() -> new Provider[] {WILDFLY_ELYTRON_PROVIDER}), Collections.singletonMap("wildfly.sasl.local-user.default-user", "$local"));
        if (saslMechanisms.isEmpty() == false) {
            saslServerFactory = new FilterMechanismSaslServerFactory(saslServerFactory, saslMechanisms::contains);
        } else {
            saslServerFactory = new SortedMechanismSaslServerFactory(saslServerFactory, DIGEST_MD5, PLAIN, JBOSS_LOCAL_USER, ANONYMOUS);
        }
        SaslAuthenticationFactory authFactory =
            SaslAuthenticationFactory.builder()
                .setSecurityDomain(securityDomain)
                .setMechanismConfigurationSelector(mechanismInformation ->
                    MechanismConfiguration.builder()
                        .addMechanismRealm(MechanismRealmConfiguration.builder().setRealmName(REALM).build())
                        .build()
                )
                .setFactory(saslServerFactory)
                .build();
        server = nsp.createServer(bindAddress, serverOptions, authFactory, null);

        Map<String, Object> configMap = new HashMap<String, Object>();
        if (excludedVersions != null) {
            configMap.put(EXCLUDED_VERSIONS, excludedVersions);
        }
        // Initialise the components that will provide JMX connectivity.
        if (mbeanServerLocator == null) {
            connectorServer = new RemotingConnectorServer(mbeanServer, endpoint, configMap, serverMessageInterceptorFactory);
            connectorServer.start();
        } else {
            delegatingServer = new DelegatingRemotingConnectorServer(mbeanServerLocator, endpoint, configMap,
                    serverMessageInterceptorFactory);
            delegatingServer.start();
        }

    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void stop() throws IOException {
        log.infof("Stopping JMX Remoting Server %s", Version.getVersionString());

        // Services using an existing Remoting installation only need to stop the JMXConnectorServer
        // to disassociate it from Remoting.
        if (connectorServer != null) {
            connectorServer.stop();
        }
        if (delegatingServer != null) {
            delegatingServer.stop();
        }
        if (server != null) {
            server.close();
        }

        if (closeEndpoint) {
            endpoint.close();
        }

//      XXX Disable this for now; the semantics need to be revisited
//        if (connectorServer != null) {
//            String[] ids = connectorServer.getConnectionIds();
//            if (ids.length > 0) {
//                throw new RuntimeException("Connections still registered with server despite being stopped.");
//            }
//        }
    }

    public static void main(String[] args) throws IOException {
        JMXRemotingConfig config = new JMXRemotingConfig();
        getPort(args, config);
        getSaslMechanism(args, config);

        JMXRemotingServer server = new JMXRemotingServer(config);
        server.start();

        System.out.println(String.format("Connect Using URL service:jmx:remoting-jmx://localhost:%d", config.port));
    }

    private static void getPort(String[] args, JMXRemotingConfig config) {
        for (String current : args) {
            if (current.startsWith(PORT_PREFIX)) {
                config.port = Integer.parseInt(current.substring(PORT_PREFIX.length()));
                return;
            }
        }
    }

    private static void getSaslMechanism(String[] args, JMXRemotingConfig config) {
        for (String current : args) {
            if (current.startsWith(SASL_MECHANISM_PREFIX)) {
                config.saslMechanisms = Collections.singleton(current.substring(SASL_MECHANISM_PREFIX.length()));
                return;
            }
        }
    }

    public static class JMXRemotingConfig {
        public Endpoint endpoint;
        public String host = "localhost";
        public int port = -1;
        public MBeanServer mbeanServer = null;
        public Set<String> saslMechanisms = null;
        public SecurityDomain securityDomain = null;
        public String excludedVersions = null;
        public MBeanServerLocator mbeanServerLocator = null;
        public ServerMessageInterceptorFactory serverMessageInterceptorFactory = null;
    }

}
