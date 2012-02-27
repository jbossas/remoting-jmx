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

package org.jboss.remoting3.jmx;

import static org.jboss.remoting3.jmx.Constants.CONNECTION_PROVIDER_URI;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.management.remote.JMXServiceURL;

/**
 * A holder for utility methods.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class Util {

    // Prevent instantiation.
    private Util() {
    }

    public static URI convert(final JMXServiceURL serviceUrl) throws IOException {
        String scheme = CONNECTION_PROVIDER_URI;
        String host = serviceUrl.getHost();
        int port = serviceUrl.getPort();

        try {
            return new URI(scheme, null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IOException("Unable to create connection URI", e);
        }
    }

}
