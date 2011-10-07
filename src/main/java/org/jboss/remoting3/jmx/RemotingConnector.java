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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class RemotingConnector implements JMXConnector {

    private static final Logger log = Logger.getLogger(RemotingConnectorServer.class);

    private final JMXServiceURL serviceUrl;
    private final Map<String, ?> environment;
    private final Endpoint endpoint;
    private final Registration registration;

    private Connection connection;
    private boolean closed = false;
    private Channel channel;
    private MBeanServerConnection serverConnection;

    RemotingConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
        this.serviceUrl = serviceURL;
        this.environment = Collections.unmodifiableMap(environment);

        // TODO - Need to review what we need to do regarding Executory, especially with notification handling.
        endpoint = Remoting.createEndpoint("endpoint", Executors.newSingleThreadExecutor(), OptionMap.EMPTY);
        final Xnio xnio = Xnio.getInstance();
        registration = endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(xnio), OptionMap.create(Options.SSL_ENABLED, false));
    }

    public void connect() throws IOException {
        connect(null);
    }

    // TODO - Do we embed the name of the channel in the URL?
    public void connect(Map<String, ?> env) throws IOException {
        // Once closed a connector is not allowed to connect again.
        // NB If a connect call fails clients are permitted to try the call again.
        if (closed) {
            throw new IOException("Connector already closed.");
        }

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
        log.info(sb.toString());

        Map<String, Object> combinedEnvironment = new HashMap(environment);
        if (env != null) {
            for (String key : env.keySet()) {
                combinedEnvironment.put(key, env.get(key));
            }
        }

        // TODO - Supported environment properties.
        // The credentials.
        // Connection timeout - maybe a total timeout?  Reducing on each await.

        // open a connection
        final IoFuture<Connection> futureConnection = endpoint.connect(convert(serviceUrl), OptionMap.create(Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE), new AnonymousCallbackHandler());
        IoFuture.Status result = futureConnection.await(5, TimeUnit.SECONDS);

        if (result == IoFuture.Status.DONE) {
            connection = futureConnection.get();
        } else if (result == IoFuture.Status.FAILED) {
            throw new IOException(futureConnection.getException());
        } else {
            throw new RuntimeException("Operation failed with status " + result);
        }

        // Now open the channel
        // TODO - Do we use the URI to specify the channel name?
        final IoFuture<Channel> futureChannel = connection.openChannel("jmx", OptionMap.EMPTY);
        result = futureChannel.await(5, TimeUnit.SECONDS);
        if (result == IoFuture.Status.DONE) {
            channel = futureChannel.get();
        } else if (result == IoFuture.Status.FAILED) {
            throw new IOException(futureChannel.getException());
        } else {
            throw new RuntimeException("Operation failed with status " + result);
        }

        final IoFuture<MBeanServerConnection> futureMBeanConnection = MBeanServerConectionFactory.createMBeanServerConnection(channel);
        result = futureMBeanConnection.await(5, TimeUnit.SECONDS);
        if (result == IoFuture.Status.DONE) {
            serverConnection = futureMBeanConnection.get();
        } else if (result == IoFuture.Status.FAILED) {
            throw new IOException(futureChannel.getException());
        } else {
            throw new RuntimeException("Operation failed with status " + result);
        }
    }

    private URI convert(final JMXServiceURL serviceUrl) throws IOException {
        String host = serviceUrl.getHost();
        int port = serviceUrl.getPort();

        try {
            return new URI("remote://" + host + ":" + port);
        } catch (URISyntaxException e) {
            throw new IOException("Unable to create connection URI", e);
        }
    }

    public MBeanServerConnection getMBeanServerConnection() throws IOException {
        System.out.println("getMBeanServerConnection()");
        return null;
    }

    public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
        System.out.println("getMBeanServerConnection(Subject)");
        return null;
    }

    public void close() throws IOException {
        System.out.println("close()");
    }

    public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        System.out.println("addConnectionNotificationListener()");
    }

    public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        System.out.println("removeConnectionNotificationListener()");
    }

    public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback)
            throws ListenerNotFoundException {
        System.out.println("removeConnectionNotificationListener()");
    }

    public String getConnectionId() throws IOException {
        System.out.println("getConnectionId()");
        return null;
    }

    /*
     *  The inner classes for use by the RemotingConnector.
     */

    private class AnonymousCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback current : callbacks) {
                if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    ncb.setName("anonymous");
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }
        }

    }

    private class ChannelCloseHandler implements CloseHandler<Channel> {

        public void handleClose(Channel channel, IOException e) {
            log.info("Client handleClose", e);
            // TODO - any clean up client side to inform that the channel is closed - notifications may need to be considered.
        }

    }


    /**
     * This class is going to need to wrap both sending messages using the correct version and waiting for the corresponding responses to be returned - again versioned.
     * <p/>
     * OR - Do I make this class versioned?
     */
    private class RemotingMBeanServerConnection implements MBeanServerConnection {

        public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
            return null;
        }

        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
            return null;
        }

        public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
            return null;
        }

        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
            return null;
        }

        public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {

        }

        public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
            return null;
        }

        public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
            return null;
        }

        public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
            return null;
        }

        public boolean isRegistered(ObjectName name) throws IOException {
            return false;
        }

        public Integer getMBeanCount() throws IOException {
            return null;
        }

        public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
            return null;
        }

        public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
            return null;
        }

        public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {

        }

        public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException, IOException {
            return null;
        }

        public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
            return null;
        }

        public String getDefaultDomain() throws IOException {
            return null;
        }

        public String[] getDomains() throws IOException {
            return new String[0];
        }

        public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {

        }

        public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {

        }

        public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {

        }

        public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {

        }

        public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {

        }

        public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {

        }

        public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
            return null;
        }

        public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
            return false;
        }
    }

}
