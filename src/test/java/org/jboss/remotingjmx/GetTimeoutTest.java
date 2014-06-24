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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.jboss.remotingjmx.Util.*;

/**
 * Test case to test the getTimeoutValue 
 *
 * @author <a href="mailto:brad.maxwell@redhat.com">Brad Maxwell</a>
 */
public class GetTimeoutTest {

    @Test
    public void testGeneric() throws Exception {

        Map env = new HashMap();

        // Test Default should be 30
        assertEquals( (Integer)30, getTimeoutValue(Timeout.GENERIC, env));

        // test GENERIC at 10
        env.put(Timeout.GENERIC.toString(), 10);
        assertEquals( (Integer)10, getTimeoutValue(Timeout.GENERIC, env));

        // test GENERIC at "10"
        env.put(Timeout.GENERIC, "10");
        assertEquals( (Integer)10, getTimeoutValue(Timeout.GENERIC, env));


        // Test CHANNEL, CONNECTION, VERSIONED_CONNECTION which should be the value of GENERIC since none of the more specific values are set

        // test CHANNEL 
        assertEquals( (Integer)10, getTimeoutValue(Timeout.CHANNEL, env));

        // test CONNECTION 
        assertEquals( (Integer)10, getTimeoutValue(Timeout.CONNECTION, env));

        // test VERSIONED_CHANNEL
        assertEquals( (Integer)10, getTimeoutValue(Timeout.VERSIONED_CONNECTION, env));
    }

    @Test
    public void testSpecificEnvironment() throws Exception {

        Map env = new HashMap();
        env.put(Timeout.CHANNEL.toString(), 40);
        env.put(Timeout.CONNECTION.toString(), 50);
        env.put(Timeout.VERSIONED_CONNECTION.toString(), 60);

        // test CHANNEL at 40
        assertEquals( (Integer)40, getTimeoutValue(Timeout.CHANNEL, env));

        // test CONNECTION at 50
        assertEquals( (Integer)50, getTimeoutValue(Timeout.CONNECTION, env));

        // test VERSIONED_CONNECTION at 60
        assertEquals( (Integer)60, getTimeoutValue(Timeout.VERSIONED_CONNECTION, env));
    }

    @Test
    public void testSystemProperties() throws Exception {

        Map env = new HashMap();
        env.put(Timeout.GENERIC.toString(), 40);
        env.put(Timeout.CHANNEL.toString(), 50);
        env.put(Timeout.CONNECTION.toString(), 60);
        env.put(Timeout.VERSIONED_CONNECTION.toString(), 70);

        try {
            System.setProperty(Timeout.GENERIC.toString(), "10");
            System.setProperty(Timeout.CHANNEL.toString(), "15");
            System.setProperty(Timeout.CONNECTION.toString(), "20");
            System.setProperty(Timeout.VERSIONED_CONNECTION.toString(), "25");

            // test GENERIC
            assertEquals( (Integer)10, getTimeoutValue(Timeout.GENERIC, env));

            // test CHANNEL 
            assertEquals( (Integer)15, getTimeoutValue(Timeout.CHANNEL, env));

            // test CONNECTION 
            assertEquals( (Integer)20, getTimeoutValue(Timeout.CONNECTION, env));

            // test VERSIONED_CONNECTION
            assertEquals( (Integer)25, getTimeoutValue(Timeout.VERSIONED_CONNECTION, env));
        } finally {
            System.clearProperty(Timeout.GENERIC.toString());
            System.clearProperty(Timeout.CHANNEL.toString());
            System.clearProperty(Timeout.CONNECTION.toString());
            System.clearProperty(Timeout.VERSIONED_CONNECTION.toString());
        }
    }

}
