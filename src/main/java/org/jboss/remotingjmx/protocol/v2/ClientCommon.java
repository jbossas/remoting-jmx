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

package org.jboss.remotingjmx.protocol.v2;

import static org.jboss.remotingjmx.Constants.TIMEOUT_KEY;
import static org.jboss.remotingjmx.protocol.v2.Constants.EXCEPTION;
import static org.jboss.remotingjmx.protocol.v2.Constants.FAILURE;
import static org.jboss.remotingjmx.protocol.v2.Constants.STRING;
import static org.jboss.remotingjmx.protocol.v2.Constants.SUCCESS;
import static org.jboss.remotingjmx.protocol.v2.Constants.VOID;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

/**
 * Base class for client side communication classes.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
abstract class ClientCommon extends Common {

    private static final Logger log = Logger.getLogger(ClientCommon.class);

    public static final int DEFAULT_TIMEOUT = 60;
    protected final int timeoutSeconds;

    ClientCommon(Channel channel, final Map<String, ?> environment) {
        super(channel);
        Integer seconds = null;
        if (environment != null && environment.containsKey(TIMEOUT_KEY)) {
            final Object timeout = environment.get(TIMEOUT_KEY);
            if (timeout instanceof Number) {
                seconds = ((Number) timeout).intValue();
            } else if (timeout instanceof String) {
                try {
                    seconds = Integer.parseInt((String) timeout);
                } catch (NumberFormatException e) {
                    log.warnf(e, "Could not parse configured timeout %s", timeout);
                }
            } else {
                log.warnf("Timeout %s configured via environment is not valid ", timeout);
            }
        } else {
            seconds = Integer.getInteger(TIMEOUT_KEY, DEFAULT_TIMEOUT);
        }
        timeoutSeconds = seconds == null ? DEFAULT_TIMEOUT : seconds;
    }

    /**
     * This Exception conversion needs to return the IOException instead of throwing it, this is so that the compiler can detect
     * that for the final Exception check something is actually thrown.
     */
    protected IOException toIoException(Exception e) {
        if (e instanceof IOException) {
            return (IOException) e;
        } else {
            return new IOException("Unexpected failure", e);
        }
    }

    protected class TypeExceptionHolder<T> {
        protected T value;
        protected Exception e;
    }

    protected abstract ClientRequestManager getClientRequestManager();

    protected abstract ClientExecutorManager getClientExecutorManager();

    protected interface MessageHandler extends Common.MessageHandler {
        boolean endReceiveLoop();
    }

    protected class MessageReceiver implements Channel.Receiver {

        @Override
        public void handleMessage(Channel channel, MessageInputStream message) {
            final DataInputStream dis = new DataInputStream(message);
            boolean endReceiveLoop = false;
            try {
                byte messageId = dis.readByte();
                final int correlationId = dis.readInt();
                log.tracef("Message Received id(%h), correlationId(%d)", messageId, correlationId);

                final Common.MessageHandler mh = getHandlerRegistry().get(messageId);
                if (mh != null) {
                    if (mh instanceof MessageHandler) {
                        endReceiveLoop = ((MessageHandler) mh).endReceiveLoop();
                    }
                    getClientExecutorManager().execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                mh.handle(dis, correlationId);
                            } catch (IOException e) {
                                log.error(e);
                            } finally {
                                IoUtils.safeClose(dis);
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
                if (endReceiveLoop == false) {
                    channel.receiveMessage(this);
                }
            }
        }

        public void handleError(Channel channel, IOException error) {
            getClientRequestManager().cancelAllRequests(error);
        }

        public void handleEnd(Channel channel) {
            getClientRequestManager().cancelAllRequests(new IOException("Connection Ended"));
        }

    }

    protected abstract class BaseResponseHandler<T> implements Common.MessageHandler {

        public void handle(DataInput input, int correlationId) {
            VersionedIoFuture<TypeExceptionHolder<T>> future = getClientRequestManager().getFuture(correlationId);
            if (future == null) {
                // spurious
                return;
            }

            try {

                TypeExceptionHolder<T> response = new TypeExceptionHolder<T>();
                byte outcome = input.readByte();
                if (outcome == SUCCESS) {
                    final byte expectedType = getExpectedType();
                    if (expectedType != VOID) {
                        byte parameterType = input.readByte();
                        if (parameterType != expectedType) {
                            throw new IOException("Unexpected response parameter received.");
                        }
                        response.value = readValue(input);
                    }
                } else if (outcome == FAILURE) {
                    byte parameterType = input.readByte();
                    if (parameterType != EXCEPTION) {
                        throw new IOException("Unexpected response parameter received.");
                    }

                    Unmarshaller unmarshaller = prepareForUnMarshalling(input);
                    response.e = unmarshaller.readObject(Exception.class);
                } else {
                    future.setException(new IOException("Outcome not understood"));
                }

                future.setResult(response);
            } catch (ClassCastException e) {
                future.setException(new IOException(e));
            } catch (ClassNotFoundException e) {
                future.setException(new IOException(e));
            } catch (IOException e) {
                future.setException(e);
            }
        }

        protected abstract byte getExpectedType();

        protected abstract T readValue(DataInput input) throws IOException;

    }

    protected class MarshalledResponseHandler<T> extends BaseResponseHandler<T> {

        private final byte expectedType;

        protected MarshalledResponseHandler(final byte expectedType) {
            this.expectedType = expectedType;
        }

        @Override
        protected byte getExpectedType() {
            return expectedType;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected T readValue(DataInput input) throws IOException {
            Unmarshaller unmarshaller = prepareForUnMarshalling(input);
            try {
                return ((T) unmarshaller.readObject());
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            } catch (ClassCastException e) {
                throw new IOException(e);
            }
        }

    }

    protected class StringResponseHandler extends BaseResponseHandler<String> {

        @Override
        protected byte getExpectedType() {
            return STRING;
        }

        @Override
        protected String readValue(DataInput input) throws IOException {
            return input.readUTF();
        }

    }

}
