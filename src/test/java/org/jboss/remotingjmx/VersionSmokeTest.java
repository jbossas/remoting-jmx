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
