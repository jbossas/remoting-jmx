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

package org.jboss.remotingjmx;

import static junit.framework.Assert.assertEquals;
import static org.jboss.remotingjmx.Util.convert;

import java.net.URI;

import javax.management.remote.JMXServiceURL;

import org.junit.Test;

/**
 * Test case to test the conversion of different addresses from a JMXServiceURL to a URI as used by Remoting.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AddressTest {

    @Test
    public void testIP4Address() throws Exception {
        String serviceAddress = "service:jmx:remoting-jmx://192.168.1.1:9999";
        JMXServiceURL serviceURL = new JMXServiceURL(serviceAddress);

        URI converted = convert(serviceURL);

        assertEquals("remote://192.168.1.1:9999", converted.toString());
    }

    @Test
    public void testIP6Address() throws Exception {
        String serviceAddress = "service:jmx:remoting-jmx://[0:0:0:0:0:0:0:1]:9999";
        JMXServiceURL serviceURL = new JMXServiceURL(serviceAddress);

        URI converted = convert(serviceURL);

        assertEquals("remote://[0:0:0:0:0:0:0:1]:9999", converted.toString());
    }

    @Test
    public void testHostAddress() throws Exception {
        String serviceAddress = "service:jmx:remoting-jmx://someHost:9999";
        JMXServiceURL serviceURL = new JMXServiceURL(serviceAddress);

        URI converted = convert(serviceURL);

        assertEquals("remote://someHost:9999", converted.toString());
    }

}
