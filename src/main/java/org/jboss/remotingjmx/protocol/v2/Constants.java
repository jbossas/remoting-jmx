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

/**
 * The version 2 constants.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class Constants {

    /*
     * Message Identifiers
     */

    static final byte CREATE_MBEAN = 0x01;
    static final byte UNREGISTER_MBEAN = 0x02;
    static final byte GET_OBJECT_INSTANCE = 0x03;
    static final byte QUERY_MBEANS = 0x04;
    static final byte QUERY_NAMES = 0x05;
    static final byte IS_REGISTERED = 0x06;
    static final byte GET_MBEAN_COUNT = 0x07;
    static final byte GET_ATTRIBUTE = 0x08;
    static final byte GET_ATTRIBUTES = 0x09;
    static final byte SET_ATTRIBUTE = 0x0A;
    static final byte SET_ATTRIBUTES = 0x0B;
    static final byte INVOKE = 0x0C;
    static final byte GET_DEFAULT_DOMAIN = 0x0D;
    static final byte GET_DOMAINS = 0x0E;
    static final byte GET_MBEAN_INFO = 0x0F;
    static final byte INSTANCE_OF = 0x10;
    static final byte ADD_NOTIFICATION_LISTENER = 0x11;
    static final byte REMOVE_NOTIFICATION_LISTENER = 0x12;
    static final byte SEND_NOTIFICATION = 0x13;
    static final byte SET_KEY_PAIR = 0x70;
    static final byte BEGIN = 0x71;

    /*
     * Response Mask
     */

    static final byte RESPONSE_MASK = (byte) 0x80;

    /*
     * Outcomes
     */

    static final byte SUCCESS = 0x00;
    static final byte FAILURE = 0x01;

    /*
     * Parameter Types
     */

    static final byte VOID = 0x00; // Not actually passed as a parameter but used internally to indicate no parameter expected.
    static final byte ATTRIBUTE = 0x01;
    static final byte ATTRIBUTE_LIST = 0x02;
    static final byte EXCEPTION = 0x03;
    static final byte OBJECT = 0x04;
    static final byte OBJECT_ARRAY = 0x05;
    static final byte OBJECT_NAME = 0x06;
    static final byte QUERY_EXP = 0x07;
    static final byte STRING = 0x08;
    static final byte STRING_ARRAY = 0x09;
    static final byte BOOLEAN = 0x0A;
    static final byte INTEGER = 0x0B;
    static final byte OBJECT_INSTANCE = 0x0C;
    static final byte SET_OBJECT_INSTANCE = 0x0D;
    static final byte MBEAN_INFO = 0x0E;
    static final byte SET_OBJECT_NAME = 0x0F;
    static final byte NOTIFICATION_FILTER = 0x10;
    static final byte NOTIFICATION = 0x11;
    static final byte INTEGER_ARRAY = 0x12;

    /*
     * General
     */

    static final String MARSHALLING_STRATEGY = "river";

}
