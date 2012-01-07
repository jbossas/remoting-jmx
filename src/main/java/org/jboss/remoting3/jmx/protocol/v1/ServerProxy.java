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
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.jmx.RemotingConnectorServer;
import org.jboss.remoting3.jmx.VersionedProxy;
import org.xnio.IoUtils;

/**
 * The VersionOne server proxy.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class ServerProxy extends Common implements VersionedProxy {

    private static final Logger log = Logger.getLogger(ServerProxy.class);

    private final Channel channel;
    private final RemotingConnectorServer server;
    private UUID connectionId;
    // Registry of handlers for the incoming messages.
    private final Map<Byte, Common.MessageHandler> handlerRegistry;

    ServerProxy(final Channel channel, final RemotingConnectorServer server) {
        super(channel);
        this.channel = channel;
        this.server = server;
        this.handlerRegistry = createHandlerRegistry();
    }

    private Map<Byte, Common.MessageHandler> createHandlerRegistry() {
        Map<Byte, Common.MessageHandler> registry = new HashMap<Byte, Common.MessageHandler>();
        registry.put(CREATE_MBEAN, new CreateMBeanHandler());
        registry.put(GET_ATTRIBUTE, new GetAttributeHandler());
        registry.put(GET_ATTRIBUTES, new GetAttributesHandler());
        registry.put(GET_DEFAULT_DOMAIN, new GetDefaultDomainHandler());
        registry.put(GET_DOMAINS, new GetDomainsHandler());
        registry.put(GET_MBEAN_COUNT, new GetMBeanCountHandler());
        registry.put(GET_MBEAN_INFO, new GetMBeanInfoHandler());
        registry.put(GET_OBJECT_INSTANCE, new GetObjectInstanceHandler());
        registry.put(INSTANCE_OF, new InstanceofHandler());
        registry.put(INVOKE, new InvokeHandler());
        registry.put(IS_REGISTERED, new IsRegisteredHandler());
        registry.put(QUERY_MBEANS, new QueryMBeansHandler());
        registry.put(QUERY_NAMES, new QueryNamesHandler());
        registry.put(SET_ATTRIBUTE, new SetAttributeHandler());
        registry.put(SET_ATTRIBUTES, new SetAttributesHandler());
        registry.put(UNREGISTER_MBEAN, new UnregisterMBeanHandler());

        return Collections.unmodifiableMap(registry);
    }

    void start() throws IOException {
        // Create a connection ID
        connectionId = UUID.randomUUID();
        log.debugf("Created connectionID %s", connectionId.toString());
        // Send ID to client
        sendConnectionId();
        // Inform server the connection is now open
        server.connectionOpened(this);
        channel.receiveMessage(new MessageReciever());
    }

    private void sendConnectionId() throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeBytes("JMX");
                output.writeUTF(connectionId.toString());
            }
        });
        log.tracef("Written connectionId %s", connectionId.toString());
    }

    public String getConnectionId() {
        return connectionId.toString();
    }

    private class MessageReciever implements Channel.Receiver {

        @Override
        public void handleMessage(final Channel channel, MessageInputStream message) {
            final DataInputStream dis = new DataInputStream(message);
            try {
                final byte messageId = dis.readByte();
                final int correlationId = dis.readInt();
                log.tracef("Message Received id(%h), correlationId(%d)", messageId, correlationId);

                final Common.MessageHandler mh = handlerRegistry.get(messageId);
                if (mh != null) {
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                mh.handle(dis, correlationId);
                            } catch (IOException e) {
                                if (correlationId != 0x00) {
                                    sendIOException(e);
                                } else {
                                    log.error("null correlationId so error not sent to client", e);
                                }
                            } finally {
                                IoUtils.safeClose(dis);
                            }
                        }

                        private void sendIOException(final IOException e) {
                            try {
                                writeResponse(e, messageId, correlationId);

                                log.tracef("[%d] %h - Success Response Sent", correlationId, messageId);
                            } catch (IOException ioe) {
                                // Here there is nothing left we can do, we know we can not sent to the client though.
                                log.error(ioe);
                            }
                        }

                    });

                } else {
                    throw new IOException("Unrecognised Message ID");
                }
            } catch (IOException e) {
                log.error(e);
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

    private void writeResponse(final byte inResponseTo, final int correlationId) throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeByte(inResponseTo ^ RESPONSE_MASK);
                output.writeInt(correlationId);
                output.writeByte(SUCCESS);
            }
        });

    }

    private void writeResponse(final Exception e, final byte inResponseTo, final int correlationId) throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeByte(inResponseTo ^ RESPONSE_MASK);
                output.writeInt(correlationId);
                output.writeByte(FAILURE);
                output.writeByte(EXCEPTION);

                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(e);
                marshaller.finish();
            }
        });

    }

    private void writeResponse(final boolean response, final byte inResponseTo, final int correlationId) throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeByte(inResponseTo ^ RESPONSE_MASK);
                output.writeInt(correlationId);
                output.writeByte(SUCCESS);
                output.writeByte(BOOLEAN);
                output.writeBoolean(response);
            }
        });

    }

    private void writeResponse(final Integer response, final byte inResponseTo, final int correlationId) throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeByte(inResponseTo ^ RESPONSE_MASK);
                output.writeInt(correlationId);
                output.writeByte(SUCCESS);
                output.writeByte(INTEGER);
                output.writeInt(response);
            }
        });

    }

    private void writeResponse(final Object response, final byte type, final byte inResponseTo, final int correlationId)
            throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeByte(inResponseTo ^ RESPONSE_MASK);
                output.writeInt(correlationId);
                output.writeByte(SUCCESS);
                output.writeByte(type);

                Marshaller marshaller = prepareForMarshalling(output);
                marshaller.writeObject(response);
                marshaller.finish();
            }
        });

    }

    private void writeResponse(final String response, final byte inResponseTo, final int correlationId) throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeByte(inResponseTo ^ RESPONSE_MASK);
                output.writeInt(correlationId);
                output.writeByte(SUCCESS);
                output.writeByte(STRING);
                output.writeUTF(response);
            }
        });

    }

    private void writeResponse(final String[] response, final byte inResponseTo, final int correlationId) throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeByte(inResponseTo ^ RESPONSE_MASK);
                output.writeInt(correlationId);
                output.writeByte(SUCCESS);
                output.writeByte(STRING_ARRAY);
                output.writeInt(response.length);
                for (String currentDomain : response) {
                    output.writeUTF(currentDomain);
                }
            }
        });

    }

    private class CreateMBeanHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, final int correlationId) throws IOException {
            log.trace("CreateMBean");
            byte paramType = input.readByte();
            if (paramType != INTEGER) {
                throw new IOException("Unexpected paramType");
            }
            int paramCount = input.readInt();
            String className = null;
            ObjectName name = null;
            ObjectName loader = null;
            Object[] params = null;
            String[] signature = null;

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            for (int i = 0; i < paramCount; i++) {
                byte param = unmarshaller.readByte();
                switch (param) {
                    case STRING:
                        if (className == null) {
                            className = unmarshaller.readUTF();
                        } else {
                            throw new IOException("Unexpected paramter");
                        }
                        break;
                    case OBJECT_NAME:
                        try {
                            if (name == null) {
                                name = unmarshaller.readObject(ObjectName.class);
                            } else if (loader == null) {
                                loader = unmarshaller.readObject(ObjectName.class);
                            } else {
                                throw new IOException("Unexpected paramter");
                            }
                        } catch (ClassNotFoundException e) {
                            throw new IOException(e);
                        }
                        break;
                    case OBJECT_ARRAY:
                        // TODO - If we have a loader may need to use it here.
                        if (params == null) {
                            int count = unmarshaller.readInt();
                            params = new Object[count];
                            for (int j = 0; j < count; j++) {
                                try {
                                    params[j] = unmarshaller.readObject();
                                } catch (ClassNotFoundException e) {
                                    throw new IOException(e);
                                }
                            }
                        } else {
                            throw new IOException("Unexpected paramter");
                        }

                        break;
                    case STRING_ARRAY:
                        if (signature == null) {
                            int count = unmarshaller.readInt();
                            signature = new String[count];
                            for (int j = 0; j < count; j++) {
                                signature[j] = unmarshaller.readUTF();
                            }
                        } else {
                            throw new IOException("Unexpected paramter");
                        }

                        break;
                    default:
                        throw new IOException("Unexpected paramter");
                }
            }

            try {
                final ObjectInstance instance;
                switch (paramCount) {
                    case 2:
                        instance = server.getMBeanServer().createMBean(className, name);
                        break;
                    case 3:
                        instance = server.getMBeanServer().createMBean(className, name, loader);
                        break;
                    case 4:
                        instance = server.getMBeanServer().createMBean(className, name, params, signature);
                        break;
                    case 5:
                        instance = server.getMBeanServer().createMBean(className, name, loader, params, signature);
                        break;
                    default:
                        throw new IOException("Unable to identify correct create method to call.");
                }

                writeResponse(instance, OBJECT_INSTANCE, CREATE_MBEAN, correlationId);

                log.tracef("[%d] CreateMBean - Success Response Sent", correlationId);
            } catch (InstanceAlreadyExistsException e) {
                writeResponse(e, CREATE_MBEAN, correlationId);
                log.tracef("[%d] CreateMBean - Failure Response Sent", correlationId);
            } catch (NotCompliantMBeanException e) {
                writeResponse(e, CREATE_MBEAN, correlationId);
                log.tracef("[%d] CreateMBean - Failure Response Sent", correlationId);
            } catch (MBeanException e) {
                writeResponse(e, CREATE_MBEAN, correlationId);
                log.tracef("[%d] CreateMBean - Failure Response Sent", correlationId);
            } catch (ReflectionException e) {
                writeResponse(e, CREATE_MBEAN, correlationId);
                log.tracef("[%d] CreateMBean - Failure Response Sent", correlationId);
            } catch (InstanceNotFoundException e) {
                writeResponse(e, CREATE_MBEAN, correlationId);
                log.tracef("[%d] CreateMBean - Failure Response Sent", correlationId);
            }

            log.tracef("[%d] CreateMBean - Failure Response Sent", correlationId);
        }
    }

    private class GetDefaultDomainHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, final int correlationId) throws IOException {
            log.trace("GetDefaultDomain");

            final String defaultDomain = server.getMBeanServer().getDefaultDomain();

            writeResponse(defaultDomain, GET_DEFAULT_DOMAIN, correlationId);

            log.tracef("[%d] CreateMBean - Success Response Sent", correlationId);
        }
    }

    private class GetDomainsHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, final int correlationId) throws IOException {
            log.trace("GetDomains");

            final String[] domains = server.getMBeanServer().getDomains();

            writeResponse(domains, GET_DOMAINS, correlationId);

            log.tracef("[%d] GetDomains - Success Response Sent", correlationId);
        }

    }

    private class GetMBeanCountHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, final int correlationId) throws IOException {
            log.trace("GetMBeanCount");

            final Integer count = server.getMBeanServer().getMBeanCount();

            writeResponse(count, GET_MBEAN_COUNT, correlationId);

            log.tracef("[%d] GetMBeanCount - Success Response Sent", correlationId);
        }

    }

    private class GetAttributeHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, final int correlationId) throws IOException {
            log.trace("GetAttribute");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            paramType = unmarshaller.readByte();
            if (paramType != STRING) {
                throw new IOException("Unexpected paramType");
            }
            String attribute = unmarshaller.readUTF();

            try {
                final Object attributeValue = server.getMBeanServer().getAttribute(objectName, attribute);

                writeResponse(attributeValue, OBJECT, GET_ATTRIBUTE, correlationId);

                log.tracef("[%d] GetAttribute - Success Response Sent", correlationId);
            } catch (AttributeNotFoundException e) {
                writeResponse(e, GET_ATTRIBUTE, correlationId);
                log.tracef("[%d] GetAttribute - Failure Response Sent", correlationId);
            } catch (InstanceNotFoundException e) {
                writeResponse(e, GET_ATTRIBUTE, correlationId);
                log.tracef("[%d] GetAttribute - Failure Response Sent", correlationId);
            } catch (MBeanException e) {
                writeResponse(e, GET_ATTRIBUTE, correlationId);
                log.tracef("[%d] GetAttribute - Failure Response Sent", correlationId);
            } catch (ReflectionException e) {
                writeResponse(e, GET_ATTRIBUTE, correlationId);
                log.tracef("[%d] GetAttribute - Failure Response Sent", correlationId);
            }
        }
    }

    private class GetAttributesHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("GetAttributes");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            paramType = unmarshaller.readByte();
            if (paramType != STRING_ARRAY) {
                throw new IOException("Unexpected paramType");
            }
            int count = unmarshaller.readInt();
            String[] attributes = new String[count];
            for (int i = 0; i < count; i++) {
                attributes[i] = unmarshaller.readUTF();
            }

            try {
                AttributeList attributeValues = server.getMBeanServer().getAttributes(objectName, attributes);

                writeResponse(attributeValues, ATTRIBUTE_LIST, GET_ATTRIBUTES, correlationId);

                log.tracef("[%d] GetAttributes - Success Response Sent", correlationId);

            } catch (InstanceNotFoundException e) {
                writeResponse(e, GET_ATTRIBUTES, correlationId);
                log.tracef("[%d] GetAttributes - Failure Response Sent", correlationId);
            } catch (ReflectionException e) {
                writeResponse(e, GET_ATTRIBUTES, correlationId);
                log.tracef("[%d] GetAttributes - Failure Response Sent", correlationId);
            }
        }
    }

    private class GetMBeanInfoHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("GetMBeanInfo");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            try {
                MBeanInfo info = server.getMBeanServer().getMBeanInfo(objectName);

                writeResponse(info, MBEAN_INFO, GET_MBEAN_INFO, correlationId);

                log.tracef("[%d] GetMBeanInfo - Success Response Sent", correlationId);
            } catch (IntrospectionException e) {
                writeResponse(e, MBEAN_INFO, correlationId);
                log.tracef("[%d] GetMBeanInfo - Failure Response Sent", correlationId);
            } catch (InstanceNotFoundException e) {
                writeResponse(e, MBEAN_INFO, correlationId);
                log.tracef("[%d] GetMBeanInfo - Failure Response Sent", correlationId);
            } catch (ReflectionException e) {
                writeResponse(e, MBEAN_INFO, correlationId);
                log.tracef("[%d] GetMBeanInfo - Failure Response Sent", correlationId);
            }
        }

    }

    private class GetObjectInstanceHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("GetObjectInstance");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            try {
                ObjectInstance objectInstance = server.getMBeanServer().getObjectInstance(objectName);

                writeResponse(objectInstance, OBJECT_INSTANCE, GET_OBJECT_INSTANCE, correlationId);

                log.tracef("[%d] GetObjectInstance - Success Response Sent", correlationId);
            } catch (InstanceNotFoundException e) {
                writeResponse(e, GET_OBJECT_INSTANCE, correlationId);
                log.tracef("[%d] GetObjectInstance - Failure Response Sent", correlationId);
            }
        }

    }

    private class InstanceofHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("IsInstanceOf");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            paramType = unmarshaller.readByte();
            if (paramType != STRING) {
                throw new IOException("Unexpected paramType");
            }
            String className = unmarshaller.readUTF();

            try {
                boolean instanceOf = server.getMBeanServer().isInstanceOf(objectName, className);

                writeResponse(instanceOf, INSTANCE_OF, correlationId);

                log.tracef("[%d] IsInstanceOf - Success Response Sent", correlationId);
            } catch (InstanceNotFoundException e) {
                writeResponse(e, INSTANCE_OF, correlationId);
                log.tracef("[%d] IsInstanceOf - Failure Response Sent", correlationId);
            }
        }

    }

    private class IsRegisteredHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("IsRegistered");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            boolean registered = server.getMBeanServer().isRegistered(objectName);

            writeResponse(registered, IS_REGISTERED, correlationId);
            log.tracef("[%d] IsRegistered - Success Response Sent", correlationId);
        }
    }

    private class InvokeHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("Invoke");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;
            String operationName;
            Object[] params;
            String[] signature;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);

                paramType = unmarshaller.readByte();
                if (paramType != STRING) {
                    throw new IOException("Unexpected paramType");
                }
                operationName = unmarshaller.readUTF();

                paramType = unmarshaller.readByte();
                if (paramType != OBJECT_ARRAY) {
                    throw new IOException("Unexpected paramType");
                }
                int count = unmarshaller.readInt();
                params = new Object[count];
                for (int i = 0; i < count; i++) {
                    params[i] = unmarshaller.readObject();
                }

                paramType = unmarshaller.readByte();
                if (paramType != STRING_ARRAY) {
                    throw new IOException("Unexpected paramType");
                }
                count = unmarshaller.readInt();
                signature = new String[count];
                for (int i = 0; i < count; i++) {
                    signature[i] = unmarshaller.readUTF();
                }
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            try {
                Object result = server.getMBeanServer().invoke(objectName, operationName, params, signature);

                writeResponse(result, OBJECT, INVOKE, correlationId);

                log.tracef("[%d] Invoke - Success Response Sent", correlationId);
            } catch (InstanceNotFoundException e) {
                writeResponse(e, INVOKE, correlationId);
                log.tracef("[%d] Invoke - Failure Response Sent", correlationId);
            } catch (ReflectionException e) {
                writeResponse(e, INVOKE, correlationId);
                log.tracef("[%d] Invoke - Failure Response Sent", correlationId);
            } catch (MBeanException e) {
                writeResponse(e, INVOKE, correlationId);
                log.tracef("[%d] Invoke - Failure Response Sent", correlationId);
            }
        }
    }

    private class QueryMBeansHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("QueryMBeans");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            ObjectName objectName;
            QueryExp query;
            try {
                Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                objectName = unmarshaller.readObject(ObjectName.class);

                paramType = unmarshaller.readByte();
                if (paramType != QUERY_EXP) {
                    throw new IOException("Unexpected paramType");
                }
                query = unmarshaller.readObject(QueryExp.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            Set<ObjectInstance> instances = server.getMBeanServer().queryMBeans(objectName, query);

            writeResponse(instances, SET_OBJECT_INSTANCE, QUERY_MBEANS, correlationId);
            log.tracef("[%d] QueryMBeans - Success Response Sent", correlationId);
        }
    }

    private class QueryNamesHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("QueryNames");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            ObjectName objectName;
            QueryExp query;
            try {
                Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                objectName = unmarshaller.readObject(ObjectName.class);

                paramType = unmarshaller.readByte();
                if (paramType != QUERY_EXP) {
                    throw new IOException("Unexpected paramType");
                }
                query = unmarshaller.readObject(QueryExp.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            Set<ObjectName> instances = server.getMBeanServer().queryNames(objectName, query);

            writeResponse(instances, SET_OBJECT_NAME, QUERY_NAMES, correlationId);

            log.tracef("[%d] QueryNames - Success Response Sent", correlationId);
        }
    }

    private class SetAttributeHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("SetAttribute");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;
            Attribute attr;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);

                paramType = unmarshaller.readByte();
                if (paramType != ATTRIBUTE) {
                    throw new IOException("Unexpected paramType");
                }
                attr = unmarshaller.readObject(Attribute.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            try {
                server.getMBeanServer().setAttribute(objectName, attr);

                writeResponse(SET_ATTRIBUTE, correlationId);

                log.tracef("[%d] SetAttribute - Success Response Sent", correlationId);
            } catch (InstanceNotFoundException e) {
                writeResponse(e, SET_ATTRIBUTE, correlationId);
                log.tracef("[%d] SetAttribute - Failure Response Sent", correlationId);
            } catch (InvalidAttributeValueException e) {
                writeResponse(e, SET_ATTRIBUTE, correlationId);
                log.tracef("[%d] SetAttribute - Failure Response Sent", correlationId);
            } catch (AttributeNotFoundException e) {
                writeResponse(e, SET_ATTRIBUTE, correlationId);
                log.tracef("[%d] SetAttribute - Failure Response Sent", correlationId);
            } catch (ReflectionException e) {
                writeResponse(e, SET_ATTRIBUTE, correlationId);
                log.tracef("[%d] SetAttribute - Failure Response Sent", correlationId);
            } catch (MBeanException e) {
                writeResponse(e, SET_ATTRIBUTE, correlationId);
                log.tracef("[%d] SetAttribute - Failure Response Sent", correlationId);
            }
        }
    }

    private class SetAttributesHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("SetAttributes");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;
            AttributeList attributes;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);

                paramType = unmarshaller.readByte();
                if (paramType != ATTRIBUTE_LIST) {
                    throw new IOException("Unexpected paramType");
                }
                attributes = unmarshaller.readObject(AttributeList.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            try {
                AttributeList attributeValues = server.getMBeanServer().setAttributes(objectName, attributes);

                writeResponse(attributeValues, ATTRIBUTE_LIST, SET_ATTRIBUTES, correlationId);

                log.tracef("[%d] SetAttributes - Success Response Sent", correlationId);
            } catch (InstanceNotFoundException e) {
                writeResponse(e, SET_ATTRIBUTES, correlationId);
                log.tracef("[%d] SetAttributes - Failure Response Sent", correlationId);
            } catch (ReflectionException e) {
                writeResponse(e, SET_ATTRIBUTES, correlationId);
                log.tracef("[%d] SetAttributes - Failure Response Sent", correlationId);
            }
        }
    }

    private class UnregisterMBeanHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            log.trace("UnregisterMBean");

            byte paramType = input.readByte();
            if (paramType != OBJECT_NAME) {
                throw new IOException("Unexpected paramType");
            }

            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            ObjectName objectName;

            try {
                objectName = unmarshaller.readObject(ObjectName.class);
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }

            try {
                server.getMBeanServer().unregisterMBean(objectName);

                writeResponse(UNREGISTER_MBEAN, correlationId);

                log.tracef("[%d] UnregisterMBean - Success Response Sent", correlationId);
            } catch (MBeanRegistrationException e) {
                writeResponse(e, UNREGISTER_MBEAN, correlationId);
                log.tracef("[%d] UnregisterMBean - Failure Response Sent", correlationId);
            } catch (InstanceNotFoundException e) {
                writeResponse(e, UNREGISTER_MBEAN, correlationId);
                log.tracef("[%d] UnregisterMBean - Failure Response Sent", correlationId);
            }
        }

    }

}