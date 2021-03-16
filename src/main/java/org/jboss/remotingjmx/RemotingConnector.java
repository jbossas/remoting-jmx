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
package org.jboss.remotingjmx;

import static java.security.AccessController.doPrivileged;
import static org.jboss.remotingjmx.Constants.CHANNEL_NAME;
import static org.jboss.remotingjmx.Constants.EXCLUDED_SASL_MECHANISMS;
import static org.jboss.remotingjmx.Constants.JBOSS_LOCAL_USER;
import static org.jboss.remotingjmx.Util.convert;
import static org.jboss.remotingjmx.Util.getTimeoutValue;
import static org.xnio.Options.SASL_POLICY_NOANONYMOUS;
import static org.xnio.Options.SASL_POLICY_NOPLAINTEXT;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.SSLContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remotingjmx.Util.Timeout;
import org.wildfly.client.config.ConfigXMLParseException;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.AuthenticationContextConfigurationClient;
import org.wildfly.security.auth.client.ElytronXmlParser;
import org.wildfly.security.auth.client.MatchRule;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Property;
import org.xnio.Sequence;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:brad.maxwell@redhat.com">Brad Maxwell</a>
 */
class RemotingConnector implements JMXConnector {

    private static final Logger log = Logger.getLogger(RemotingConnectorServer.class);
    private static final AuthenticationContextConfigurationClient AUTH_CONFIGURATION_CLIENT = doPrivileged(AuthenticationContextConfigurationClient.ACTION);

    private final JMXServiceURL serviceUrl;
    private final Map<String, ?> environment;

    private Endpoint endpoint;
    private Connection connection;
    private ConnectorState state = ConnectorState.UNUSED;
    private Channel channel;
    private VersionedConnection versionedConnection;
    private ShutDownHook shutDownHook;

    RemotingConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
        this.serviceUrl = serviceURL;
        this.environment = Collections.unmodifiableMap(environment);
    }

    public void connect() throws IOException {
        connect(null);
    }

    public void connect(Map<String, ?> env) throws IOException {
        try {
            internalConnect(env);
        } catch (Exception e) {
            close();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                // Added for completeness but this line should not be reachable.
                throw new IOException(e);
            }
        }
    }

    private synchronized void internalConnect(Map<String, ?> env) throws IOException {
        // Once closed a connector is not allowed to connect again.
        // NB If a connect call fails clients are permitted to try the call again.
        switch (state) {
            case CLOSED:
                throw new IOException("Connector already closed.");
            case OPEN:
                return;
            case UNUSED: // Just to complete the switch.
        }

        if (log.isTraceEnabled()) {
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
            log.trace(sb.toString());
        }

        Map<String, Object> combinedEnvironment = new HashMap<String, Object>(environment);
        if (env != null) {
            for (String key : env.keySet()) {
                combinedEnvironment.put(key, env.get(key));
            }
        }

        Connection connection = internalRemotingConnect(combinedEnvironment);

        String serviceName = serviceUrl.getURLPath();
        if (serviceName.startsWith("/") || serviceName.startsWith(";")) {
            serviceName = serviceName.substring(1);
            if (serviceName.contains("?")) {
                // Drop any query parameters when identifying the service name.
                serviceName = serviceName.substring(0, serviceName.indexOf('?'));
            }
        }
        if (serviceName.length() == 0) {
            serviceName = CHANNEL_NAME;
        }

        // Now open the channel
        final IoFuture<Channel> futureChannel = connection.openChannel(serviceName, OptionMap.EMPTY);
        IoFuture.Status result = futureChannel.await(getTimeoutValue(Timeout.CHANNEL, combinedEnvironment), TimeUnit.SECONDS);
        if (result == IoFuture.Status.DONE) {
            channel = futureChannel.get();
        } else if (result == IoFuture.Status.FAILED) {
            throw futureChannel.getException();
        } else {
            throw new IOException("Operation failed with status " + result);
        }

        versionedConnection = VersionedConectionFactory.createVersionedConnection(channel, env, serviceUrl);
        state = ConnectorState.OPEN;
        Runtime.getRuntime().addShutdownHook((shutDownHook = new ShutDownHook()));
    }

    /**
     * Internal method to either return the Remoting connection provided within the environment settings or establish and return
     * a new connection.
     *
     * The Connection will only be cached by this RemotingConnector if this RemotingConnector actually creates it - that way we
     * will not accidentally close a connection being managed elsewhere.
     *
     * Note: This method does not verify that the supplied Connection matches the address in the URL.
     *
     * @return The Remoting connection to use to connect to the channel.
     * @throws IOException - If there is any failure establishing the connection.
     */
    private Connection internalRemotingConnect(Map<String, ?> env) throws IOException {
        if (env.containsKey(Connection.class.getName())) {
            // DO NOT Cache this.
            return (Connection) env.get(Connection.class.getName());
        }

        endpoint = Endpoint.builder().setEndpointName("endpoint").build();

        Set<String> disabledMechanisms = new HashSet<String>();

        final URI uri = convert(serviceUrl);

        AuthenticationContext captured;
        // Determine if an authentication configuration was defined in the environment
        final Object authConfig = env.get("wildfly.config.url");
        URI authConfigUri = null;
        if (authConfig != null) {
            if (authConfig instanceof URI) {
                authConfigUri = (URI) authConfig;
            } else {
                authConfigUri = URI.create(authConfig.toString());
            }
        }
        if (authConfigUri == null) {
            captured = AuthenticationContext.captureCurrent();
        } else {
            try {
                captured = ElytronXmlParser.parseAuthenticationClientConfiguration(authConfigUri).create();
            } catch (GeneralSecurityException | ConfigXMLParseException e) {
                throw new IOException("Failed to parse authentication configuration " + authConfig, e);
            }
        }
        AuthenticationConfiguration mergedConfiguration = AUTH_CONFIGURATION_CLIENT.getAuthenticationConfiguration(uri, captured);

        SSLContext tempContext = null;
        try {
            tempContext = AUTH_CONFIGURATION_CLIENT.getSSLContext(uri, captured);
        } catch (GeneralSecurityException e) {
            log.trace("No SSLContext available", e);
        }

        // The credentials.
        CallbackHandler handler;
        handler = (CallbackHandler) env.get(CallbackHandler.class.getName());
        if (handler != null) {
            mergedConfiguration = mergedConfiguration.useCallbackHandler(handler);
        } else {
            if (env.containsKey(CREDENTIALS)) {
                String[] credentials = (String[]) env.get(CREDENTIALS);
                final String username = credentials[0];
                final String password = credentials[1];
                mergedConfiguration = mergedConfiguration.useName(username).usePassword(password).useRealm(null);
                if (username != null && username.trim().length() > 0) {
                    disabledMechanisms.add(JBOSS_LOCAL_USER);
                }
            }
        }

        Object list;
        if (env.containsKey(EXCLUDED_SASL_MECHANISMS) && (list = env.get(EXCLUDED_SASL_MECHANISMS)) != null) {
           String[] mechanisms;
           if (list instanceof String[]) {
               mechanisms = (String[])list;
           } else {
               mechanisms = list.toString().split(",");
           }

           disabledMechanisms.addAll(Arrays.asList(mechanisms));
        }

        // open a connection
        AuthenticationContext context = AuthenticationContext.empty().with(MatchRule.ALL, mergedConfiguration);
        final SSLContext sslContext = tempContext;
        context = sslContext != null ? context.withSsl(MatchRule.ALL, () -> sslContext) : context;
        final IoFuture<Connection> futureConnection = endpoint.connect(convert(serviceUrl), getOptionMap(disabledMechanisms), context);
        IoFuture.Status result = futureConnection.await(getTimeoutValue(Timeout.CONNECTION, env), TimeUnit.SECONDS);

        if (result == IoFuture.Status.DONE) {
            connection = futureConnection.get();
        } else if (result == IoFuture.Status.FAILED) {
            throw futureConnection.getException();
        } else {
            throw new IOException("Operation failed with status " + result);
        }

        return connection;
    }

    private OptionMap getOptionMap(Set<String> disabledMechanisms) {
        OptionMap.Builder builder = OptionMap.builder();
        builder.set(SASL_POLICY_NOANONYMOUS, Boolean.FALSE);
        builder.set(SASL_POLICY_NOPLAINTEXT, Boolean.FALSE);

        List<Property> tempProperties = new ArrayList<Property>(1);
        tempProperties.add(Property.of("jboss.sasl.local-user.quiet-auth", "true"));
        builder.set(Options.SASL_PROPERTIES, Sequence.of(tempProperties));

        builder.set(Options.SSL_ENABLED, true);
        builder.set(Options.SSL_STARTTLS, true);

        if (disabledMechanisms != null && disabledMechanisms.size() > 0) {
            builder.set(Options.SASL_DISALLOWED_MECHANISMS, Sequence.of(disabledMechanisms));
        }

        return builder.getMap();
    }

    private void verifyConnected() throws IOException {
        if (state == ConnectorState.CLOSED) {
            throw new IOException("Connector already closed.");
        } else if (versionedConnection == null) {
            throw new IOException("Connector not connected.");
        }
    }

    public MBeanServerConnection getMBeanServerConnection() throws IOException {
        log.trace("getMBeanServerConnection()");

        return getMBeanServerConnection(null);
    }

    public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
        log.trace("getMBeanServerConnection(Subject)");
        verifyConnected();

        return versionedConnection.getMBeanServerConnection(delegationSubject);
    }

    public synchronized void close() throws IOException {
        log.trace("close()");
        switch (state) {
            case UNUSED:
                /*
                 * If the connection has not quite opened we do want to clean up anything partially opened but we
                 * don't want to flag as 'CLOSED' as another connection could be attempted using this Connector.
                 */
                break;
            case CLOSED:
                return;
            case OPEN:
                state = ConnectorState.CLOSED;
        }

        final ShutDownHook shutDownHook;
        if ((shutDownHook = this.shutDownHook) != null) {
            Runtime.getRuntime().removeShutdownHook(shutDownHook);
            this.shutDownHook = null;
        }

        safeClose(versionedConnection);
        this.versionedConnection = null;
        safeClose(channel);
        this.channel = null;
        safeClose(connection);
        this.connection = null;
        safeClose(endpoint);
        this.endpoint = null;
    }

    private void safeClose(final Channel channel) {
        if (channel != null) {
            try {
                channel.writeShutdown();
            } catch (Exception ignored) {
            }
        }
        safeClose((Closeable) channel);
    }

    private void safeClose(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        log.trace("addConnectionNotificationListener()");
    }

    public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        log.trace("removeConnectionNotificationListener()");
    }

    public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback)
            throws ListenerNotFoundException {
        log.trace("removeConnectionNotificationListener()");
    }

    public String getConnectionId() throws IOException {
        log.trace("getConnectionId()");
        verifyConnected();

        String connectionId = versionedConnection.getConnectionId();

        log.debugf("Our connection id is '%s'", connectionId);
        return connectionId;
    }

    private class ShutDownHook extends Thread {
        private ShutDownHook() {
            super(new Runnable() {

                public void run() {
                    if (state == ConnectorState.OPEN) {
                        try {
                            shutDownHook = null;
                            close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            });
        }
    }

    private enum ConnectorState {
        UNUSED, OPEN, CLOSED;
    }

}
