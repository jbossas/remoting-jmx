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

package org.jboss.remotingjmx.protocol.v2;

import static org.jboss.remotingjmx.protocol.v2.Constants.MARSHALLING_STRATEGY;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import org.jboss.marshalling.AbstractClassResolver;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.protocol.CancellableDataOutputStream;
import org.xnio.IoUtils;

/**
 * The common base class for both sides of the connection.
 *
 * (The marshalling initialisation is originally copied as-is from the ejb-client project)
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
abstract class Common {

    private final Channel channel;

    private final MarshallerFactory marshallerFactory;

    Common(Channel channel) {
        marshallerFactory = Marshalling.getProvidedMarshallerFactory(MARSHALLING_STRATEGY);
        if (marshallerFactory == null) {
            throw new RuntimeException("Could not find a marshaller factory for " + MARSHALLING_STRATEGY
                    + " marshalling strategy");
        }
        this.channel = channel;
    }

    abstract Map<Byte, Common.MessageHandler> getHandlerRegistry();

    /**
     * Creates and returns a {@link org.jboss.marshalling.Marshaller} which is ready to be used for marshalling. The
     * {@link org.jboss.marshalling.Marshaller#start(org.jboss.marshalling.ByteOutput)} will be invoked by this method, to use
     * the passed {@link java.io.DataOutput dataOutput}, before returning the marshaller.
     *
     * @param dataOutput The {@link java.io.DataOutput} to which the data will be marshalled
     * @return
     * @throws IOException
     */
    protected org.jboss.marshalling.Marshaller prepareForMarshalling(final DataOutput dataOutput) throws IOException {
        final org.jboss.marshalling.Marshaller marshaller = this.getMarshaller(marshallerFactory);
        final OutputStream outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                final int byteToWrite = b & 0xff;
                dataOutput.write(byteToWrite);
            }
        };
        final ByteOutput byteOutput = Marshalling.createByteOutput(outputStream);
        // start the marshaller
        marshaller.start(byteOutput);

        return marshaller;
    }

    /**
     * Creates and returns a {@link org.jboss.marshalling.Unmarshaller} which is ready to be used for unmarshalling. The
     * {@link org.jboss.marshalling.Unmarshaller#start(org.jboss.marshalling.ByteInput)} will be invoked by this method, to use
     * the passed {@link java.io.DataInput dataInput}, before returning the unmarshaller.
     *
     * This unmarshaller will use the context class loader to resolve any classes.
     *
     * @param dataInput The data input from which to unmarshall
     * @return
     * @throws IOException
     */
    protected Unmarshaller prepareForUnMarshalling(final DataInput dataInput) throws IOException {
        return prepareForUnMarshalling(dataInput, DefaultClassResolver.INSTANCE);
    }

    /**
     * Creates and returns a {@link org.jboss.marshalling.Unmarshaller} which is ready to be used for unmarshalling. The
     * {@link org.jboss.marshalling.Unmarshaller#start(org.jboss.marshalling.ByteInput)} will be invoked by this method, to use
     * the passed {@link java.io.DataInput dataInput}, before returning the unmarshaller.
     *
     * @param dataInput The data input from which to unmarshall
     * @param classResolver The class resolver to use for unmarshalling
     * @return
     * @throws IOException
     */
    protected Unmarshaller prepareForUnMarshalling(final DataInput dataInput, final ClassResolver classResolver)
            throws IOException {
        final Unmarshaller unmarshaller = this.getUnMarshaller(marshallerFactory, classResolver);
        final InputStream is = new InputStream() {
            @Override
            public int read() throws IOException {
                try {

                    final int b = dataInput.readByte();
                    return b & 0xff;
                } catch (EOFException eof) {
                    return -1;
                }
            }
        };
        final ByteInput byteInput = Marshalling.createByteInput(is);
        // start the unmarshaller
        unmarshaller.start(byteInput);

        return unmarshaller;
    }

    /**
     * Creates and returns a {@link org.jboss.marshalling.Marshaller}
     *
     * @param marshallerFactory The marshaller factory
     * @return
     * @throws IOException
     */
    private org.jboss.marshalling.Marshaller getMarshaller(final org.jboss.marshalling.MarshallerFactory marshallerFactory)
            throws IOException {
        final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
        // TODO - Do we need an Object and Class table?
        marshallingConfiguration.setVersion(2);

        // TODO - Will need classloading server side to be in context of bean being called.

        return marshallerFactory.createMarshaller(marshallingConfiguration);
    }

    /**
     * Creates and returns a {@link Unmarshaller}
     *
     *
     * @param marshallerFactory The marshaller factory
     * @return
     * @throws IOException
     */
    private Unmarshaller getUnMarshaller(final MarshallerFactory marshallerFactory, final ClassResolver classResolver)
            throws IOException {
        final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
        marshallingConfiguration.setVersion(2);
        marshallingConfiguration.setClassResolver(classResolver);
        return marshallerFactory.createUnmarshaller(marshallingConfiguration);
    }

    protected void write(MessageWriter writer) throws IOException {
        CancellableDataOutputStream output = new CancellableDataOutputStream(channel.writeMessage());
        try {
            writer.write(output);
        } catch (IOException e) {
            output.cancel();
            throw e;
        } finally {
            IoUtils.safeClose(output);
        }
    }

    interface MessageWriter {
        void write(DataOutput output) throws IOException;
    }

    interface MessageHandler {
        void handle(DataInput input, int correlationId) throws IOException;
    }

    /**
     * Default class resolver, intended for use on the client side. It will use the context class loader if available, otherwise
     * it will fall back to the class loader use to load this class.
     */
    private static final class DefaultClassResolver extends AbstractClassResolver {

        public static final DefaultClassResolver INSTANCE = new DefaultClassResolver();

        private static final PrivilegedAction<ClassLoader> classLoaderAction = new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        };

        /** {@inheritDoc} */
        protected ClassLoader getClassLoader() {
            final SecurityManager sm = System.getSecurityManager();
            final ClassLoader classLoader;
            if (sm != null) {
                classLoader = AccessController.doPrivileged(classLoaderAction);
            } else {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (classLoader == null) {
                return DefaultClassResolver.class.getClassLoader();
            } else {
                return classLoader;
            }
        }
    }

}
