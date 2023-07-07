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

import static org.jboss.remotingjmx.protocol.v2.Constants.EXCEPTION;
import static org.jboss.remotingjmx.protocol.v2.Constants.FAILURE;
import static org.jboss.remotingjmx.protocol.v2.Constants.RESPONSE_MASK;
import static org.jboss.remotingjmx.protocol.v2.Constants.STRING;
import static org.jboss.remotingjmx.protocol.v2.Constants.SUCCESS;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.Executor;

import javax.management.JMRuntimeException;

import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remotingjmx.ServerMessageInterceptor;
import org.jboss.remotingjmx.ServerMessageInterceptor.Event;
import org.xnio.IoUtils;

/**
 * An extension of Common to hold anything common to both ParameterProxy and ServerProxy but not ClientConnection.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class ServerCommon extends Common {

    private static final Logger log = Logger.getLogger(ServerCommon.class);

    private final Executor executor;
    private final ServerMessageInterceptor serverMessageInterceptor;

    ServerCommon(Channel channel, Executor executor, ServerMessageInterceptor serverMessageInterceptor) {
        super(channel);
        this.executor = executor;
        this.serverMessageInterceptor = serverMessageInterceptor;
    }

    protected void sendWelcomeMessage() throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeBytes("JMX");
            }
        });
        log.tracef("Written welcome message");
    }

    protected void writeResponse(final Exception e, final byte inResponseTo, final int correlationId) throws IOException {
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

    protected void writeResponse(final byte inResponseTo, final int correlationId) throws IOException {
        write(new MessageWriter() {

            @Override
            public void write(DataOutput output) throws IOException {
                output.writeByte(inResponseTo ^ RESPONSE_MASK);
                output.writeInt(correlationId);
                output.writeByte(SUCCESS);
            }
        });
    }

    protected void writeResponse(final String response, final byte inResponseTo, final int correlationId) throws IOException {
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

    abstract void end();

    abstract class MessageHandler implements Common.MessageHandler {
        boolean endReceiveLoop() {
            return false;
        }
    }

    protected class MessageReciever implements Channel.Receiver {

        @Override
        public void handleMessage(final Channel channel, MessageInputStream message) {
            final DataInputStream dis = new DataInputStream(message);
            boolean endReceiveLoop = false;
            try {
                final byte messageId = dis.readByte();
                final int correlationId = dis.readInt();
                log.tracef("Message Received id(%h), correlationId(%d)", messageId, correlationId);

                final Common.MessageHandler mh = getHandlerRegistry().get(messageId);
                if (mh instanceof MessageHandler) {
                    endReceiveLoop = ((MessageHandler) mh).endReceiveLoop();
                }
                if (mh != null) {
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                serverMessageInterceptor.handleEvent(new Event() {

                                    @Override
                                    public void run() throws IOException {
                                        mh.handle(dis, correlationId);
                                    }
                                });
                            } catch (Throwable t) {
                                if (correlationId != 0x00) {
                                    Exception response;
                                    if (t instanceof IOException) {
                                        response = (Exception) t;
                                    } else if (t instanceof JMRuntimeException) {
                                        response = (Exception) t;
                                    } else {
                                        response = new IOException("Internal server error.");
                                        log.warn("Unexpected internal error", t);
                                    }

                                    sendIOException(response);
                                } else {
                                    log.error("null correlationId so error not sent to client", t);
                                }
                            } finally {
                                IoUtils.safeClose(dis);
                            }
                        }

                        private void sendIOException(final Exception e) {
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
                if (endReceiveLoop == false) {
                    channel.receiveMessage(this);
                }
            }
        }

        public void handleError(Channel channel, IOException error) {
            log.warn("Channel closing due to error", error);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        serverMessageInterceptor.handleEvent(new Event() {
                            @Override
                            public void run() {
                                end();
                            }
                        });
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            });
        }

        @Override
        public void handleEnd(final Channel channel) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        serverMessageInterceptor.handleEvent(new Event() {
                            @Override
                            public void run() {
                                end();
                            }
                        });
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            });
        }

    }

}
