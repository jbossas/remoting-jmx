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

/**
 * The version 1 constants.
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

    /*
     * General
     */

    static final String MARSHALLING_STRATEGY = "river";

}
