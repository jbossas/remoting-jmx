/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.remoting3.jmx.protocol.v1;

import static org.jboss.remoting3.jmx.protocol.v1.Constants.ATTRIBUTE;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.ATTRIBUTE_LIST;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.BOOLEAN;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.CREATE_MBEAN;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.EXCEPTION;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.FAILURE;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.GET_ATTRIBUTE;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.GET_ATTRIBUTES;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.GET_DEFAULT_DOMAIN;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.GET_DOMAINS;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.GET_MBEAN_COUNT;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.GET_MBEAN_INFO;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.GET_OBJECT_INSTANCE;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.INSTANCE_OF;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.INTEGER;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.INVOKE;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.IS_REGISTERED;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.MBEAN_INFO;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.OBJECT;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.OBJECT_ARRAY;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.OBJECT_INSTANCE;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.OBJECT_NAME;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.QUERY_EXP;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.QUERY_MBEANS;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.QUERY_NAMES;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.RESPONSE_MASK;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.SET_ATTRIBUTE;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.SET_ATTRIBUTES;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.SET_OBJECT_INSTANCE;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.SET_OBJECT_NAME;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.STRING;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.STRING_ARRAY;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.SUCCESS;
import static org.jboss.remoting3.jmx.protocol.v1.Constants.UNREGISTER_MBEAN;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import javax.security.auth.Subject;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.jmx.VersionedConnection;
import org.xnio.IoFuture;
import org.xnio.IoUtils;

/**
 * The VersionOne client connection.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class ClientConnection extends Common implements VersionedConnection {

    private final Channel channel;
    // Registry of handlers for the incoming messages.
    private final Map<Byte, Common.MessageHandler> handlerRegistry;
    private String connectionId;
    private MBeanServerConnection mbeanServerConnection;

    private int nextCorrelationId = 1;

    /**
     * The in-progress requests awaiting a response.
     */
    private final Map<Integer, VersionedIoFuture> requests = new HashMap<Integer, VersionedIoFuture>();

    ClientConnection(final Channel channel) {
        this.channel = channel;
        handlerRegistry = createHandlerRegistry();
    }

    private Map<Byte, Common.MessageHandler> createHandlerRegistry() {
        Map<Byte, Common.MessageHandler> registry = new HashMap<Byte, Common.MessageHandler>();
        registry.put((byte) (CREATE_MBEAN ^ RESPONSE_MASK), new ObjectInstanceResponseHandler());
        registry.put((byte) (GET_ATTRIBUTE ^ RESPONSE_MASK), new ObjectResponseHandler());
        registry.put((byte) (GET_ATTRIBUTES ^ RESPONSE_MASK), new AttributeListResponseHandler());
        registry.put((byte) (GET_DEFAULT_DOMAIN ^ RESPONSE_MASK), new GetDefaultDomainResponseHandler());
        registry.put((byte) (GET_DOMAINS ^ RESPONSE_MASK), new GetDomainsResponseHandler());
        registry.put((byte) (GET_MBEAN_COUNT ^ RESPONSE_MASK), new GetMBeanCountResponseHandler());
        registry.put((byte) (GET_MBEAN_INFO ^ RESPONSE_MASK), new GetMBeanInfoResponseHandler());
        registry.put((byte) (GET_OBJECT_INSTANCE ^ RESPONSE_MASK), new ObjectInstanceResponseHandler());
        registry.put((byte) (INSTANCE_OF ^ RESPONSE_MASK), new IsInstanceOfResponseHandler());
        registry.put((byte) (IS_REGISTERED ^ RESPONSE_MASK), new IsRegisteredResponseHandler());
        registry.put((byte) (INVOKE ^ RESPONSE_MASK), new ObjectResponseHandler());
        registry.put((byte) (QUERY_MBEANS ^ RESPONSE_MASK), new QueryMBeansResponseHandler());
        registry.put((byte) (QUERY_NAMES ^ RESPONSE_MASK), new QueryNamesResponseHandler());
        registry.put((byte) (SET_ATTRIBUTE ^ RESPONSE_MASK), new VoidResponseHandler());
        registry.put((byte) (SET_ATTRIBUTES ^ RESPONSE_MASK), new AttributeListResponseHandler());
        registry.put((byte) (UNREGISTER_MBEAN ^ RESPONSE_MASK), new VoidResponseHandler());

        return Collections.unmodifiableMap(registry);
    }

    void start() throws IOException {
        sendVersionHeader();
        IoFuture<String> futureConnectionId = ConnectionIdReceiver.getConnectionId(channel);
        IoFuture.Status result = futureConnectionId.await(5, TimeUnit.SECONDS);
        switch (result) {
            case DONE:
                connectionId = futureConnectionId.get();
                mbeanServerConnection = new TheConnection();
                channel.receiveMessage(new MessageReceiver());
                break;
            case FAILED:
                throw futureConnectionId.getException();
            default:
                throw new IOException("Unable to obtain connectionId, status=" + result.toString());
        }
    }

    private void sendVersionHeader() throws IOException {
        DataOutputStream dos = new DataOutputStream(channel.writeMessage());
        try {
            dos.writeBytes("JMX");
            dos.writeByte(VersionOne.getVersionIdentifier());
        } finally {
            dos.close();
        }
    }

    public String getConnectionId() {
        if (connectionId == null) {
            throw new IllegalStateException("Connection ID not set");
        }

        return connectionId;
    }

    public MBeanServerConnection getMBeanServerConnection(Subject subject) {
        // TODO - Correct behaviour if called and no connection.
        if (subject != null) {
            throw new UnsupportedOperationException("Subject delegation not supported for getMBeanServerConnection");
        }

        // It is normal for only a single instance of MBeanServerConnection to be used.
        return mbeanServerConnection;
    }

    public void close() {
        // TODO - Messages to close the connection.
    }

    /**
     * Get the next correlation ID, returning to the beginning once all integers have been used.
     * <p/>
     * THIS METHOD IS NOT TO BE USED DIRECTLY WHERE A CORRELATION ID NEEDS TO BE RESERVED.
     *
     * @return The next correlationId.
     */
    private synchronized int getNextCorrelationId() {
        int next = nextCorrelationId++;
        // After the maximum integer start back at the beginning.
        if (next < 0) {
            nextCorrelationId = 2;
            next = 1;
        }
        return next;
    }

    /**
     * Reserves a correlation ID by taking the next value and ensuring it is stored in the Map.
     *
     * @return the next reserved correlation ID
     */
    private synchronized int reserveNextCorrelationId(VersionedIoFuture future) {
        Integer next = getNextCorrelationId();

        // Not likely but possible to use all IDs and start back at beginning while
        // old request still in progress.
        while (requests.containsKey(next)) {
            next = getNextCorrelationId();
        }
        requests.put(next, future);

        return next;
    }

    private synchronized <T> VersionedIoFuture<T> getFuture(int correlationId) {
        // TODO - How to check this?
        return requests.get(correlationId);
    }

    private synchronized void releaseCorrelationId(int correlationId) {
        // TODO - Will maybe move to not removing by default and timeout failed requests.
        requests.remove(correlationId);
    }

    private class MessageReceiver implements Channel.Receiver {

        @Override
        public void handleMessage(Channel channel, MessageInputStream message) {
            final DataInputStream dis = new DataInputStream(message);
            try {
                byte messageId = dis.readByte();
                final Common.MessageHandler mh = handlerRegistry.get(messageId);
                if (mh != null) {
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                mh.handle(dis);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                IoUtils.safeClose(dis);
                            }
                        }

                    });

                } else {
                    throw new IOException("Unrecognised Message ID");
                }
            } catch (IOException e) {
                e.printStackTrace();
                IoUtils.safeClose(dis);
            } finally {
                // TODO - Propper shut down logic.
                channel.receiveMessage(this);
            }
        }

        @Override
        public void handleError(Channel channel, IOException error) {
            // TODO Auto-generated method stub

        }

        @Override
        public void handleEnd(Channel channel) {
            // TODO Auto-generated method stub

        }

    }

    private class TheConnection implements MBeanServerConnection {
        // TODO - Consider a proxy so the specific methods only need to marshall their specific
        // portion of the protocol.

        public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException,
                InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                IOException {
            VersionedIoFuture<TypeExceptionHolder<ObjectInstance>> future = new VersionedIoFuture<TypeExceptionHolder<ObjectInstance>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(CREATE_MBEAN);
                output.writeInt(correlationId);

                output.writeByte(INTEGER);
                output.writeInt(2); // Sening 2 parameters.

                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeByte(STRING);
                marshaller.writeUTF(className);

                marshaller.writeByte(OBJECT_NAME);

                marshaller.writeObject(name);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<ObjectInstance> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        reflectionException(response.e);
                        instanceAlreadyExistsException(response.e);
                        mbeanRegistrationException(response.e);
                        mbeanException(response.e);
                        notCompliantMBeanException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to obtain createMBean, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException,
                InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                InstanceNotFoundException, IOException {
            VersionedIoFuture<TypeExceptionHolder<ObjectInstance>> future = new VersionedIoFuture<TypeExceptionHolder<ObjectInstance>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(CREATE_MBEAN);
                output.writeInt(correlationId);

                output.writeByte(INTEGER);
                output.writeInt(3); // Sending 3 parameters.

                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeByte(STRING);
                marshaller.writeUTF(className);

                marshaller.writeByte(OBJECT_NAME);
                marshaller.writeObject(name);

                marshaller.writeByte(OBJECT_NAME);
                marshaller.writeObject(loaderName);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<ObjectInstance> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        reflectionException(response.e);
                        instanceAlreadyExistsException(response.e);
                        mbeanRegistrationException(response.e);
                        mbeanException(response.e);
                        notCompliantMBeanException(response.e);
                        instanceNotFoundException(response.e);
                        ioException(response.e);
                        // Allow fall through to default if no response and exception not thrown by this point.
                    default:
                        throw new IOException("Unable to obtain isRegistered, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
                throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
                NotCompliantMBeanException, IOException {
            VersionedIoFuture<TypeExceptionHolder<ObjectInstance>> future = new VersionedIoFuture<TypeExceptionHolder<ObjectInstance>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(CREATE_MBEAN);
                output.writeInt(correlationId);

                output.writeByte(INTEGER);
                output.writeInt(4); // Sending 4 parameters.

                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeByte(STRING);
                marshaller.writeUTF(className);

                marshaller.writeByte(OBJECT_NAME);

                marshaller.writeObject(name);

                marshaller.writeByte(OBJECT_ARRAY);
                marshaller.writeInt(params.length);
                for (Object current : params) {
                    marshaller.writeObject(current);
                }

                marshaller.writeByte(STRING_ARRAY);
                marshaller.writeInt(signature.length);
                for (String current : signature) {
                    marshaller.writeUTF(current);
                }

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<ObjectInstance> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        reflectionException(response.e);
                        instanceAlreadyExistsException(response.e);
                        mbeanRegistrationException(response.e);
                        mbeanException(response.e);
                        notCompliantMBeanException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to invoke createMBean, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
                String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
            VersionedIoFuture<TypeExceptionHolder<ObjectInstance>> future = new VersionedIoFuture<TypeExceptionHolder<ObjectInstance>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(CREATE_MBEAN);
                output.writeInt(correlationId);

                output.writeByte(INTEGER);
                output.writeInt(5); // Sending 5 parameters.

                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeByte(STRING);
                marshaller.writeUTF(className);

                marshaller.writeByte(OBJECT_NAME);
                marshaller.writeObject(name);

                marshaller.writeByte(OBJECT_NAME);
                marshaller.writeObject(loaderName);

                marshaller.writeByte(OBJECT_ARRAY);
                marshaller.writeInt(params.length);
                for (Object current : params) {
                    marshaller.writeObject(current);
                }

                marshaller.writeByte(STRING_ARRAY);
                marshaller.writeInt(signature.length);
                for (String current : signature) {
                    marshaller.writeUTF(current);
                }

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<ObjectInstance> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        reflectionException(response.e);
                        instanceAlreadyExistsException(response.e);
                        mbeanRegistrationException(response.e);
                        mbeanException(response.e);
                        notCompliantMBeanException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to invoke createMBean, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
            VersionedIoFuture<TypeExceptionHolder<Void>> future = new VersionedIoFuture<TypeExceptionHolder<Void>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(UNREGISTER_MBEAN);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<Void> response = future.get();

                        instanceNotFoundException(response.e);
                        mbeanRegistrationException(response.e);
                        ioException(response.e);
                        // Allow fall through to default if no response and exception not thrown by this point.
                    default:
                        // We don't expect a response so not an error to reach this point.
                        return;
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
            VersionedIoFuture<TypeExceptionHolder<ObjectInstance>> future = new VersionedIoFuture<TypeExceptionHolder<ObjectInstance>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(GET_OBJECT_INSTANCE);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<ObjectInstance> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        instanceNotFoundException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to invoke getObjectInstance, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
            VersionedIoFuture<Set<ObjectInstance>> future = new VersionedIoFuture<Set<ObjectInstance>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(QUERY_MBEANS);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.writeByte(QUERY_EXP);
                marshaller.writeObject(query);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        return future.get();
                    default:
                        throw new IOException("Unable to invoke queryMBeans, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
            VersionedIoFuture<Set<ObjectName>> future = new VersionedIoFuture<Set<ObjectName>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(QUERY_NAMES);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.writeByte(QUERY_EXP);
                marshaller.writeObject(query);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        return future.get();
                    default:
                        throw new IOException("Unable to obtain isRegistered, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public boolean isRegistered(ObjectName name) throws IOException {
            VersionedIoFuture<Boolean> future = new VersionedIoFuture<Boolean>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(IS_REGISTERED);
                output.writeInt(correlationId);
                output.writeByte(OBJECT_NAME);

                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case DONE:
                        return future.get();
                    case FAILED:
                        throw future.getException();
                    default:
                        throw new IOException("Unable to obtain isRegistered, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public Integer getMBeanCount() throws IOException {
            VersionedIoFuture<Integer> future = new VersionedIoFuture<Integer>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(GET_MBEAN_COUNT);
                output.writeInt(correlationId);

                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case DONE:
                        return future.get();
                    case FAILED:
                        throw future.getException();
                    default:
                        throw new IOException("Unable to obtain MBeanCount, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException,
                InstanceNotFoundException, ReflectionException, IOException {
            VersionedIoFuture<TypeExceptionHolder<Object>> future = new VersionedIoFuture<TypeExceptionHolder<Object>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(GET_ATTRIBUTE);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.writeByte(STRING);
                marshaller.writeUTF(attribute);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<Object> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        mbeanException(response.e);
                        attributeNotFoundException(response.e);
                        instanceNotFoundException(response.e);
                        reflectionException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to obtain isRegistered, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException,
                ReflectionException, IOException {
            VersionedIoFuture<TypeExceptionHolder<AttributeList>> future = new VersionedIoFuture<TypeExceptionHolder<AttributeList>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(GET_ATTRIBUTES);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.writeByte(STRING_ARRAY);
                marshaller.writeInt(attributes.length);
                for (String current : attributes) {
                    marshaller.writeUTF(current);
                }

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<AttributeList> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        instanceNotFoundException(response.e);
                        reflectionException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to invoke getAttributes, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException,
                AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
            VersionedIoFuture<TypeExceptionHolder<Void>> future = new VersionedIoFuture<TypeExceptionHolder<Void>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(SET_ATTRIBUTE);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.writeByte(ATTRIBUTE);
                marshaller.writeObject(attribute);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<Void> response = future.get();

                        instanceNotFoundException(response.e);
                        attributeNotFoundException(response.e);
                        invalidAttributeValueException(response.e);
                        mbeanException(response.e);
                        reflectionException(response.e);
                        ioException(response.e);
                        // Allow fall through to default if no response and exception not thrown by this point.
                    default:
                        // No return type and was no Exception.
                        return;
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException,
                ReflectionException, IOException {
            VersionedIoFuture<TypeExceptionHolder<AttributeList>> future = new VersionedIoFuture<TypeExceptionHolder<AttributeList>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(SET_ATTRIBUTES);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.writeByte(ATTRIBUTE_LIST);
                marshaller.writeObject(attributes);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<AttributeList> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        instanceNotFoundException(response.e);
                        reflectionException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to invoke setAttributes, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
                throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
            VersionedIoFuture<TypeExceptionHolder<Object>> future = new VersionedIoFuture<TypeExceptionHolder<Object>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(INVOKE);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.writeByte(STRING);
                marshaller.writeUTF(operationName);

                marshaller.writeByte(OBJECT_ARRAY);
                if (params != null) {
                    marshaller.writeInt(params.length);
                    for (Object current : params) {
                        marshaller.writeObject(current);
                    }
                } else {
                    marshaller.writeInt(0);
                }

                marshaller.writeByte(STRING_ARRAY);
                if (signature != null) {
                    marshaller.writeInt(signature.length);
                    for (String current : signature) {
                        marshaller.writeUTF(current);
                    }
                } else {
                    marshaller.writeInt(0);
                }

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<Object> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        instanceNotFoundException(response.e);
                        mbeanException(response.e);
                        reflectionException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to invoke invoke(), status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public String getDefaultDomain() throws IOException {
            VersionedIoFuture<String> future = new VersionedIoFuture<String>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(GET_DEFAULT_DOMAIN);
                output.writeInt(correlationId);

                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case DONE:
                        return future.get();
                    case FAILED:
                        throw future.getException();
                    default:
                        throw new IOException("Unable to obtain DefaultDomain, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public String[] getDomains() throws IOException {
            VersionedIoFuture<String[]> future = new VersionedIoFuture<String[]>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(GET_DOMAINS);
                output.writeInt(correlationId);

                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case DONE:
                        return future.get();
                    case FAILED:
                        throw future.getException();
                    default:
                        throw new IOException("Unable to obtain Domains, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
                Object handback) throws InstanceNotFoundException, IOException {
            // TODO Auto-generated method stub

        }

        public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
                throws InstanceNotFoundException, IOException {
            // TODO Auto-generated method stub

        }

        public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException,
                ListenerNotFoundException, IOException {
            // TODO Auto-generated method stub

        }

        public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
                throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            // TODO Auto-generated method stub

        }

        public void removeNotificationListener(ObjectName name, NotificationListener listener)
                throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            // TODO Auto-generated method stub

        }

        public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
                Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            // TODO Auto-generated method stub

        }

        public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException,
                ReflectionException, IOException {
            VersionedIoFuture<TypeExceptionHolder<MBeanInfo>> future = new VersionedIoFuture<TypeExceptionHolder<MBeanInfo>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(GET_MBEAN_INFO);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.close();
                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<MBeanInfo> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        instanceNotFoundException(response.e);
                        introspectionException(response.e);
                        reflectionException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to obtain isRegistered, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
            VersionedIoFuture<TypeExceptionHolder<Boolean>> future = new VersionedIoFuture<TypeExceptionHolder<Boolean>>();
            int correlationId = reserveNextCorrelationId(future);
            try {
                DataOutputStream output = new DataOutputStream(channel.writeMessage());
                output.writeByte(INSTANCE_OF);
                output.writeInt(correlationId);

                output.writeByte(OBJECT_NAME);
                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(name);

                marshaller.writeByte(STRING);
                marshaller.writeUTF(className);

                output.close();

                IoFuture.Status result = future.await(5, TimeUnit.SECONDS);
                switch (result) {
                    case FAILED:
                        throw future.getException();
                    case DONE:
                        TypeExceptionHolder<Boolean> response = future.get();
                        if (response.e == null) {
                            return response.value;
                        }
                        instanceNotFoundException(response.e);
                        ioException(response.e);
                        throw new RuntimeException(response.e);
                    default:
                        throw new IOException("Unable to obtain isRegistered, status=" + result.toString());
                }
            } finally {
                releaseCorrelationId(correlationId);
            }
        }

        private void attributeNotFoundException(Exception e) throws AttributeNotFoundException {
            if (e != null && e instanceof AttributeNotFoundException) {
                throw (AttributeNotFoundException) e;
            }
        }

        private void instanceAlreadyExistsException(Exception e) throws InstanceAlreadyExistsException {
            if (e != null && e instanceof InstanceAlreadyExistsException) {
                throw (InstanceAlreadyExistsException) e;
            }
        }

        private void instanceNotFoundException(Exception e) throws InstanceNotFoundException {
            if (e != null && e instanceof InstanceNotFoundException) {
                throw (InstanceNotFoundException) e;
            }
        }

        private void introspectionException(Exception e) throws IntrospectionException {
            if (e != null && e instanceof IntrospectionException) {
                throw (IntrospectionException) e;
            }
        }

        private void invalidAttributeValueException(Exception e) throws InvalidAttributeValueException {
            if (e != null && e instanceof InvalidAttributeValueException) {
                throw (InvalidAttributeValueException) e;
            }
        }

        private void mbeanRegistrationException(Exception e) throws MBeanRegistrationException {
            if (e != null && e instanceof MBeanRegistrationException) {
                throw (MBeanRegistrationException) e;
            }
        }

        private void mbeanException(Exception e) throws MBeanException {
            if (e != null && e instanceof MBeanException) {
                throw (MBeanException) e;
            }
        }

        private void notCompliantMBeanException(Exception e) throws NotCompliantMBeanException {
            if (e != null && e instanceof NotCompliantMBeanException) {
                throw (NotCompliantMBeanException) e;
            }
        }

        private void reflectionException(Exception e) throws ReflectionException {
            if (e != null && e instanceof ReflectionException) {
                throw (ReflectionException) e;
            }
        }

        private void ioException(Exception e) throws IOException {
            if (e != null) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOException("Unexpected failure", e);
                }
            }
        }

    }

    private class TypeExceptionHolder<T> {
        private T value;
        private Exception e;
    }

    private class GetDefaultDomainResponseHandler implements Common.MessageHandler {

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<String> future = getFuture(correlationId);

            try {
                byte outcome = input.readByte();
                if (outcome != SUCCESS) {
                    throw new IOException("Call failed, reason unknown - check the server logs.");
                }
                byte parameterType = input.readByte();
                if (parameterType != STRING) {
                    throw new IOException("Unexpected response parameter received.");
                }

                future.setResult(input.readUTF());
            } catch (IOException e) {
                future.setException(e);
            }
        }

    }

    private class GetDomainsResponseHandler implements Common.MessageHandler {

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<String[]> future = getFuture(correlationId);

            try {
                byte outcome = input.readByte();
                if (outcome != SUCCESS) {
                    throw new IOException("Call failed, reason unknown - check the server logs.");
                }
                byte parameterType = input.readByte();
                if (parameterType != STRING_ARRAY) {
                    throw new IOException("Unexpected response parameter received.");
                }
                int length = input.readInt();
                String[] domains = new String[length];
                for (int i = 0; i < length; i++) {
                    domains[i] = input.readUTF();
                }

                future.setResult(domains);
            } catch (IOException e) {
                future.setException(e);
            }
        }

    }

    private class GetMBeanCountResponseHandler implements Common.MessageHandler {

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<Integer> future = getFuture(correlationId);

            try {
                byte outcome = input.readByte();
                if (outcome != SUCCESS) {
                    throw new IOException("Call failed, reason unknown - check the server logs.");
                }
                byte parameterType = input.readByte();
                if (parameterType != INTEGER) {
                    throw new IOException("Unexpected response parameter received.");
                }

                future.setResult(input.readInt());
            } catch (IOException e) {
                future.setException(e);
            }
        }

    }

    private class GetMBeanInfoResponseHandler implements Common.MessageHandler {

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<TypeExceptionHolder<MBeanInfo>> future = getFuture(correlationId);

            try {
                TypeExceptionHolder<MBeanInfo> response = new TypeExceptionHolder<MBeanInfo>();
                byte outcome = input.readByte();
                if (outcome == SUCCESS) {
                    byte parameterType = input.readByte();
                    if (parameterType != MBEAN_INFO) {
                        throw new IOException("Unexpected response parameter received.");
                    }
                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.value = unmarshaller.readObject(MBeanInfo.class);
                } else if (outcome == FAILURE) {
                    byte parameterType = input.readByte();
                    if (parameterType != EXCEPTION) {
                        throw new IOException("Unexpected response parameter received.");
                    }

                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.e = unmarshaller.readObject(Exception.class);
                } else {
                    throw new IOException("Outcome not understood");
                }

                future.setResult(response);
            } catch (ClassNotFoundException e) {
                future.setException(new IOException(e));
            } catch (IOException e) {
                future.setException(e);
            }
        }
    }

    private class IsInstanceOfResponseHandler implements Common.MessageHandler {

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<TypeExceptionHolder<Boolean>> future = getFuture(correlationId);

            try {
                TypeExceptionHolder<Boolean> response = new TypeExceptionHolder<Boolean>();
                byte outcome = input.readByte();
                if (outcome == SUCCESS) {
                    byte parameterType = input.readByte();
                    if (parameterType != BOOLEAN) {
                        throw new IOException("Unexpected response parameter received.");
                    }
                    response.value = input.readBoolean();
                } else if (outcome == FAILURE) {
                    byte parameterType = input.readByte();
                    if (parameterType != EXCEPTION) {
                        throw new IOException("Unexpected response parameter received.");
                    }

                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.e = unmarshaller.readObject(InstanceNotFoundException.class);
                } else {
                    throw new IOException("Outcome not understood");
                }

                future.setResult(response);
            } catch (ClassNotFoundException e) {
                future.setException(new IOException(e));
            } catch (IOException e) {
                future.setException(e);
            }
        }
    }

    private class IsRegisteredResponseHandler implements Common.MessageHandler {

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<Boolean> future = getFuture(correlationId);

            try {
                byte outcome = input.readByte();
                if (outcome != SUCCESS) {
                    throw new IOException("Call failed, reason unknown - check the server logs.");
                }
                byte parameterType = input.readByte();
                if (parameterType != BOOLEAN) {
                    throw new IOException("Unexpected response parameter received.");
                }

                future.setResult(input.readBoolean());
            } catch (IOException e) {
                future.setException(e);
            }
        }

    }

    private class AttributeListResponseHandler implements Common.MessageHandler {

        /*
         * This handler is shared by a few different methods as they have either a Object response or they have an exception as
         * a response.
         */

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<TypeExceptionHolder<AttributeList>> future = getFuture(correlationId);

            try {
                TypeExceptionHolder<AttributeList> response = new TypeExceptionHolder<AttributeList>();
                byte outcome = input.readByte();
                if (outcome == SUCCESS) {
                    byte parameterType = input.readByte();
                    if (parameterType != ATTRIBUTE_LIST) {
                        throw new IOException("Unexpected response parameter received.");
                    }
                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.value = unmarshaller.readObject(AttributeList.class);
                } else if (outcome == FAILURE) {
                    byte parameterType = input.readByte();
                    if (parameterType != EXCEPTION) {
                        throw new IOException("Unexpected response parameter received.");
                    }

                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.e = unmarshaller.readObject(Exception.class);
                } else {
                    throw new IOException("Outcome not understood");
                }

                future.setResult(response);
            } catch (ClassNotFoundException e) {
                future.setException(new IOException(e));
            } catch (IOException e) {
                future.setException(e);
            }
        }
    }

    private class ObjectResponseHandler implements Common.MessageHandler {

        /*
         * This handler is shared by a few different methods as they have either a Object response or they have an exception as
         * a response.
         */

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<TypeExceptionHolder<Object>> future = getFuture(correlationId);

            try {
                TypeExceptionHolder<Object> response = new TypeExceptionHolder<Object>();
                byte outcome = input.readByte();
                if (outcome == SUCCESS) {
                    byte parameterType = input.readByte();
                    if (parameterType != OBJECT) {
                        throw new IOException("Unexpected response parameter received.");
                    }
                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.value = unmarshaller.readObject();
                } else if (outcome == FAILURE) {
                    byte parameterType = input.readByte();
                    if (parameterType != EXCEPTION) {
                        throw new IOException("Unexpected response parameter received.");
                    }

                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.e = unmarshaller.readObject(Exception.class);
                } else {
                    throw new IOException("Outcome not understood");
                }

                future.setResult(response);
            } catch (ClassNotFoundException e) {
                future.setException(new IOException(e));
            } catch (IOException e) {
                future.setException(e);
            }
        }
    }

    private class ObjectInstanceResponseHandler implements Common.MessageHandler {

        /*
         * This handler is shared by a few different methods as they have either a ObjectInstance response or they have an
         * exception as a response.
         */

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<TypeExceptionHolder<ObjectInstance>> future = getFuture(correlationId);

            try {
                TypeExceptionHolder<ObjectInstance> response = new TypeExceptionHolder<ObjectInstance>();
                byte outcome = input.readByte();
                if (outcome == SUCCESS) {
                    byte parameterType = input.readByte();
                    if (parameterType != OBJECT_INSTANCE) {
                        throw new IOException("Unexpected response parameter received.");
                    }
                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.value = unmarshaller.readObject(ObjectInstance.class);
                } else if (outcome == FAILURE) {
                    byte parameterType = input.readByte();
                    if (parameterType != EXCEPTION) {
                        throw new IOException("Unexpected response parameter received.");
                    }

                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.e = unmarshaller.readObject(Exception.class);
                } else {
                    throw new IOException("Outcome not understood");
                }

                future.setResult(response);
            } catch (ClassNotFoundException e) {
                future.setException(new IOException(e));
            } catch (IOException e) {
                future.setException(e);
            }
        }
    }

    private class QueryMBeansResponseHandler implements Common.MessageHandler {

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<Set<ObjectInstance>> future = getFuture(correlationId);

            try {
                byte outcome = input.readByte();
                if (outcome != SUCCESS) {
                    throw new IOException("Call failed, reason unknown - check the server logs.");
                }
                byte parameterType = input.readByte();
                if (parameterType != SET_OBJECT_INSTANCE) {
                    throw new IOException("Unexpected response parameter received.");
                }

                Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                future.setResult(unmarshaller.readObject(Set.class));
            } catch (ClassNotFoundException e) {
                future.setException(new IOException(e));
            } catch (IOException e) {
                future.setException(e);
            }
        }

    }

    private class QueryNamesResponseHandler implements Common.MessageHandler {

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<Set<ObjectName>> future = getFuture(correlationId);

            try {
                byte outcome = input.readByte();
                if (outcome != SUCCESS) {
                    throw new IOException("Call failed, reason unknown - check the server logs.");
                }
                byte parameterType = input.readByte();
                if (parameterType != SET_OBJECT_NAME) {
                    throw new IOException("Unexpected response parameter received.");
                }

                Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                future.setResult(unmarshaller.readObject(Set.class));
            } catch (ClassNotFoundException e) {
                future.setException(new IOException(e));
            } catch (IOException e) {
                future.setException(e);
            }
        }

    }

    private class VoidResponseHandler implements Common.MessageHandler {

        /*
         * This handler is shared by a few different methods as they have either a ObjectInstance response or they have an
         * exception as a response.
         */

        public void handle(DataInput input) throws IOException {
            int correlationId = input.readInt();
            VersionedIoFuture<TypeExceptionHolder<Void>> future = getFuture(correlationId);

            try {
                TypeExceptionHolder<Void> response = new TypeExceptionHolder<Void>();
                byte outcome = input.readByte();
                if (outcome == FAILURE) {
                    byte parameterType = input.readByte();
                    if (parameterType != EXCEPTION) {
                        throw new IOException("Unexpected response parameter received.");
                    }

                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.e = unmarshaller.readObject(Exception.class);
                } else if (outcome != SUCCESS) {
                    throw new IOException("Outcome not understood");
                }

                future.setResult(response);
            } catch (ClassNotFoundException e) {
                future.setException(new IOException(e));
            } catch (IOException e) {
                future.setException(e);
            }
        }
    }

}