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
package org.jboss.remotingjmx.common;

import static org.jboss.remotingjmx.Constants.EXCLUDED_VERSIONS;
import static org.xnio.Options.SASL_MECHANISMS;
import static org.xnio.Options.SASL_POLICY_NOANONYMOUS;
import static org.xnio.Options.SASL_POLICY_NOPLAINTEXT;
import static org.xnio.Options.SASL_PROPERTIES;
import static org.xnio.Options.SSL_ENABLED;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.jboss.remotingjmx.DelegatingRemotingConnectorServer;
import org.jboss.remotingjmx.MBeanServerLocator;
import org.jboss.remotingjmx.RemotingConnectorServer;
import org.jboss.remotingjmx.ServerMessageInterceptorFactory;
import org.jboss.remotingjmx.Version;
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
import org.wildfly.security.sasl.util.PropertiesSaslServerFactory;
import org.wildfly.security.sasl.util.SaslFactories;
import org.xnio.OptionMap;
import org.xnio.OptionMap.Builder;
import org.xnio.Property;
import org.xnio.Sequence;

/**
 * A test server to test exposing the local MBeanServer using Remoting.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class JMXRemotingServer {

    public static final String ANONYMOUS = "ANONYMOUS";
    public static final String DIGEST_MD5 = "DIGEST-MD5";
    public static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";
    public static final String PLAIN = "PLAIN";

    private static final String REALM = "JMX_Test_Realm";

    static final String REALM_PROPERTY = "com.sun.security.sasl.digest.realm";
    static final String PRE_DIGESTED_PROPERTY = "org.jboss.sasl.digest.pre_digested";
    static final String LOCAL_DEFAULT_USER = "jboss.sasl.local-user.default-user";
    static final String LOCAL_USER_CHALLENGE_PATH = "jboss.sasl.local-user.challenge-path";

    private static final String DOLLAR_LOCAL = "$local";

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
            final SimpleMapBackedSecurityRealm realm = new SimpleMapBackedSecurityRealm();
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
    }

    public void start() throws IOException {
        log.infof("Starting JMX Remoting Server %s", Version.getVersionString());

        // Initialise general Remoting - this step would be implemented elsewhere when
        // running within an application server.
        endpoint = Endpoint.getCurrent();

        final NetworkServerProvider nsp = endpoint.getConnectionProviderInterface("remote", NetworkServerProvider.class);
        final SocketAddress bindAddress = new InetSocketAddress(host, listenerPort);
        final OptionMap serverOptions = createOptionMap();

        SaslAuthenticationFactory authFactory =
            SaslAuthenticationFactory.builder()
                .setSecurityDomain(securityDomain)
                .setMechanismConfigurationSelector(mechanismInformation ->
                    MechanismConfiguration.builder()
                        .addMechanismRealm(MechanismRealmConfiguration.builder().setRealmName(REALM).build())
                        .build()
                )
                .setFactory(new PropertiesSaslServerFactory(SaslFactories.getElytronSaslServerFactory(), Collections.singletonMap("wildfly.sasl.local-user.default-user", "$local")))
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

    // This duplicates the RealmSecurityProvider of AS7 to mimic the same security set-up
    private OptionMap createOptionMap() {
        List<String> mechanisms = new LinkedList<String>();
        Set<Property> properties = new HashSet<Property>();
        Builder builder = OptionMap.builder();

        if (saslMechanisms.contains(JBOSS_LOCAL_USER)) {
            mechanisms.add(JBOSS_LOCAL_USER);
            builder.set(SASL_POLICY_NOPLAINTEXT, false);
            properties.add(Property.of(LOCAL_DEFAULT_USER, DOLLAR_LOCAL));
        }

        if (saslMechanisms.contains(DIGEST_MD5)) {
            mechanisms.add(DIGEST_MD5);
            properties.add(Property.of(REALM_PROPERTY, REALM));

        }

        if (saslMechanisms.contains(PLAIN)) {
            mechanisms.add(PLAIN);
            builder.set(SASL_POLICY_NOPLAINTEXT, false);
        }

        if (saslMechanisms.isEmpty() || saslMechanisms.contains(ANONYMOUS)) {
            mechanisms.add(ANONYMOUS);
            builder.set(SASL_POLICY_NOANONYMOUS, false);
        }

        // TODO - SSL Options will be added in a subsequent task.
        builder.set(SSL_ENABLED, false);

        builder.set(SASL_MECHANISMS, Sequence.of(mechanisms));
        builder.set(SASL_PROPERTIES, Sequence.of(properties));

        return builder.getMap();
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
