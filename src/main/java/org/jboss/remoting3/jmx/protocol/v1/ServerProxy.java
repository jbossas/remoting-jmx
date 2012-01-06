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
import java.util.UUID;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;

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
        log.infof("Created connectionID %s", connectionId.toString());
        // Send ID to client
        sendConnectionId();
        // Inform server the connection is now open
        server.connectionOpened(this);
        channel.receiveMessage(new MessageReciever());
    }

    private void sendConnectionId() throws IOException {
        DataOutputStream dos = new DataOutputStream(channel.writeMessage());
        try {
            dos.writeBytes("JMX");
            dos.writeUTF(connectionId.toString());
        } finally {
            dos.close();
            log.infof("Written connectionId %s", connectionId.toString());
        }
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
                                    e.printStackTrace();
                                }
                            } finally {
                                IoUtils.safeClose(dis);
                            }
                        }

                        private void sendIOException(IOException e) {
                            DataOutputStream dos = null;

                            try {
                                dos = new DataOutputStream(channel.writeMessage());
                                dos.writeByte(messageId ^ RESPONSE_MASK);
                                dos.writeInt(correlationId);
                                dos.writeByte(FAILURE);
                                dos.writeByte(EXCEPTION);

                                Marshaller marshaller = prepareForMarshalling(dos);
                                marshaller.writeObject(e);
                                marshaller.finish();
                            } catch (IOException ioe) {
                                // Here there is nothing left we can do, we know we can not sent to the client though.
                                ioe.printStackTrace();
                            } finally {
                                IoUtils.safeClose(dos);
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

    private class CreateMBeanHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** CreateMBean");
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

            DataOutputStream dos = null;
            try {
                ObjectInstance instance;
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

                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(CREATE_MBEAN ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
                dos.writeByte(OBJECT_INSTANCE);

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(instance);
                marshaller.finish();
            } catch (Exception e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(UNREGISTER_MBEAN ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                if (e instanceof RuntimeException) {
                    // We only want to send back the known checked exceptions.
                    e.printStackTrace();
                    e = new IOException("Unexpected internal failure.");
                }

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }
    }

    private class GetDefaultDomainHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** GetDefaultDomain");

            String defaultDomain = server.getMBeanServer().getDefaultDomain();

            DataOutputStream dos = new DataOutputStream(channel.writeMessage());
            try {
                dos.writeByte(GET_DEFAULT_DOMAIN ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
                dos.writeByte(STRING);
                dos.writeUTF(defaultDomain);
            } finally {
                IoUtils.safeClose(dos);
            }

        }

    }

    private class GetDomainsHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** GetDomains");

            String[] domains = server.getMBeanServer().getDomains();

            DataOutputStream dos = new DataOutputStream(channel.writeMessage());
            try {
                dos.writeByte(GET_DOMAINS ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
                dos.writeByte(STRING_ARRAY);
                dos.writeInt(domains.length);
                for (String currentDomain : domains) {
                    dos.writeUTF(currentDomain);
                }
            } finally {
                IoUtils.safeClose(dos);
            }

        }

    }

    private class GetMBeanCountHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** GetMBeanCount");

            Integer count = server.getMBeanServer().getMBeanCount();

            DataOutputStream dos = new DataOutputStream(channel.writeMessage());
            try {
                dos.writeByte(GET_MBEAN_COUNT ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
                dos.writeByte(INTEGER);
                dos.writeInt(count);
            } finally {
                IoUtils.safeClose(dos);
            }

        }

    }

    private class GetAttributeHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** GetAttribute");

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

            DataOutputStream dos = null;
            try {
                Object attributeValue = server.getMBeanServer().getAttribute(objectName, attribute);
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(GET_ATTRIBUTE ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);

                dos.writeByte(OBJECT);
                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(attributeValue);
                marshaller.finish();
            } catch (Exception e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(GET_ATTRIBUTE ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                if (e instanceof RuntimeException) {
                    // We only want to send back the known checked exceptions.
                    e.printStackTrace();
                    e = new IOException("Unexpected internal failure.");
                }

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }
    }

    private class GetAttributesHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** GetAttributes");

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

            DataOutputStream dos = null;
            try {
                AttributeList attributeValues = server.getMBeanServer().getAttributes(objectName, attributes);
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(GET_ATTRIBUTES ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);

                dos.writeByte(ATTRIBUTE_LIST);
                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(attributeValues);
                marshaller.finish();
            } catch (Exception e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(GET_ATTRIBUTES ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                if (e instanceof RuntimeException) {
                    // We only want to send back the known checked exceptions.
                    e.printStackTrace();
                    e = new IOException("Unexpected internal failure.");
                }

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }
    }

    private class GetMBeanInfoHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** GetMBeanInfo");

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

            DataOutputStream dos = null;
            try {
                MBeanInfo info = server.getMBeanServer().getMBeanInfo(objectName);
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(GET_MBEAN_INFO ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);

                dos.writeByte(MBEAN_INFO);
                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(info);
                marshaller.finish();
            } catch (Exception e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(GET_MBEAN_INFO ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                if (e instanceof RuntimeException) {
                    // We only want to send back the known checked exceptions.
                    e.printStackTrace();
                    e = new IOException("Unexpected internal failure.");
                }

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }

    }

    private class GetObjectInstanceHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** GetObjectInstance");

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

            DataOutputStream dos = null;
            try {
                ObjectInstance objectInstance = server.getMBeanServer().getObjectInstance(objectName);
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(GET_OBJECT_INSTANCE ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
                dos.writeByte(OBJECT_INSTANCE);
                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(objectInstance);
                marshaller.finish();
            } catch (InstanceNotFoundException e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(INSTANCE_OF ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }

    }

    private class InstanceofHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** IsInstanceOf");

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

            DataOutputStream dos = null;
            try {
                boolean instanceOf = server.getMBeanServer().isInstanceOf(objectName, className);
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(INSTANCE_OF ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
                dos.writeByte(BOOLEAN);
                dos.writeBoolean(instanceOf);
            } catch (InstanceNotFoundException e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(INSTANCE_OF ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }

    }

    private class IsRegisteredHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** IsRegistered");

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

            DataOutputStream dos = new DataOutputStream(channel.writeMessage());
            try {
                dos.writeByte(IS_REGISTERED ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
                dos.writeByte(BOOLEAN);
                dos.writeBoolean(registered);
            } finally {
                IoUtils.safeClose(dos);
            }

        }
    }

    private class InvokeHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** Invoke");

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

            DataOutputStream dos = null;
            try {
                Object result = server.getMBeanServer().invoke(objectName, operationName, params, signature);
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(INVOKE ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);

                dos.writeByte(OBJECT);
                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(result);
                marshaller.finish();
            } catch (Exception e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(INVOKE ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                if (e instanceof RuntimeException) {
                    // We only want to send back the known checked exceptions.
                    e.printStackTrace();
                    e = new IOException("Unexpected internal failure.");
                }

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }
    }

    private class QueryMBeansHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** QueryMBeans");

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

            DataOutputStream dos = new DataOutputStream(channel.writeMessage());
            try {
                dos.writeByte(QUERY_MBEANS ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
                dos.writeByte(SET_OBJECT_INSTANCE);

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(instances);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }

        }
    }

    private class QueryNamesHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** QueryNames");

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

            DataOutputStream dos = new DataOutputStream(channel.writeMessage());
            try {
                dos.writeByte(QUERY_NAMES ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
                dos.writeByte(SET_OBJECT_NAME);

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(instances);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }

        }
    }

    private class SetAttributeHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** SetAttribute");

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

            DataOutputStream dos = null;
            try {
                server.getMBeanServer().setAttribute(objectName, attr);
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(SET_ATTRIBUTE ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
            } catch (Exception e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(SET_ATTRIBUTE ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                if (e instanceof RuntimeException) {
                    // We only want to send back the known checked exceptions.
                    e.printStackTrace();
                    e = new IOException("Unexpected internal failure.");
                }

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }
    }

    private class SetAttributesHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** SetAttributes");

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

            DataOutputStream dos = null;
            try {
                AttributeList attributeValues = server.getMBeanServer().setAttributes(objectName, attributes);
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(SET_ATTRIBUTES ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);

                dos.writeByte(ATTRIBUTE_LIST);
                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(attributeValues);
                marshaller.finish();
            } catch (Exception e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(SET_ATTRIBUTES ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                if (e instanceof RuntimeException) {
                    // We only want to send back the known checked exceptions.
                    e.printStackTrace();
                    e = new IOException("Unexpected internal failure.");
                }

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }
    }

    private class UnregisterMBeanHandler implements Common.MessageHandler {

        @Override
        public void handle(DataInput input, int correlationId) throws IOException {
            System.out.println("** UnregisterMBean");

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

            DataOutputStream dos = null;
            try {
                server.getMBeanServer().unregisterMBean(objectName);
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(UNREGISTER_MBEAN ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(SUCCESS);
            } catch (Exception e) {
                dos = new DataOutputStream(channel.writeMessage());
                dos.writeByte(UNREGISTER_MBEAN ^ RESPONSE_MASK);
                dos.writeInt(correlationId);
                dos.writeByte(FAILURE);
                dos.writeByte(EXCEPTION);

                if (e instanceof RuntimeException) {
                    // We only want to send back the known checked exceptions.
                    e.printStackTrace();
                    e = new IOException("Unexpected internal failure.");
                }

                Marshaller marshaller = prepareForMarshalling(dos);
                marshaller.writeObject(e);
                marshaller.finish();
            } finally {
                IoUtils.safeClose(dos);
            }
        }

    }

}