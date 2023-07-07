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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServerConnection;

import org.jboss.remotingjmx.protocol.Versions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case to ensure all versions of the protocols are used.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class VersionSmokeTest extends AbstractTestBase {

    // We still want the set-up as in the base class but we override the
    // methods so we can control when these are call.

    @BeforeClass
    public static void setupServer() {
    }

    @AfterClass
    public static void tearDownServer() {
    }

    @Before
    public void connect() throws IOException {
    }

    @After
    public void disconnect() throws IOException {
    }

    @Test
    public void testVersions() throws Exception {
        Versions versions = new Versions(Collections.EMPTY_MAP);
        Set<Byte> supportedVersions = versions.getSupportedVersions();
        Set<Byte> secondSupportedVersions = new HashSet<Byte>(supportedVersions);
        for (Byte current : supportedVersions) {
            StringBuilder sb = null;
            for (Byte toAdd : secondSupportedVersions) {
                if (toAdd != current) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    } else {
                        sb.append(",");
                    }
                    sb.append(toAdd);
                }
            }
            assertNotNull("At least one version should be excluded.", sb);

            String excludedVersions = sb.toString();
            super.setupServer(excludedVersions);
            super.connect(excludedVersions);
            smokeTest();
            super.disconnect();
            super.tearDownServer();
        }
    }

    protected void smokeTest() throws Exception {
        String defaultDomain = mbeanServer.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Direct Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        MBeanServerConnection connection = connector.getMBeanServerConnection();
        defaultDomain = connection.getDefaultDomain();
        assertNotNull("defaultDomain", defaultDomain);
        assertEquals("Remote Access Default Domain", DEFAULT_DOMAIN, defaultDomain);

        Integer count = mbeanServer.getMBeanCount();
        assertNotNull("count", count);
        assertEquals("Direct Access MBeanCount", (Integer) 1, count);

        count = connection.getMBeanCount();
        assertNotNull("count", count);
        assertEquals("Remote Access MBeanCount", (Integer) 1, count);
    }

}
