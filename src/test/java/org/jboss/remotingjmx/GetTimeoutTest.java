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

import static org.jboss.remotingjmx.Util.getTimeoutValue;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.jboss.remotingjmx.Util.Timeout;
import org.junit.Test;

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
