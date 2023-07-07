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

package org.jboss.remotingjmx;

import static org.jboss.remotingjmx.Util.convert;
import static org.junit.Assert.assertEquals;

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
