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

import static org.jboss.remotingjmx.Constants.CONNECTION_PROVIDER_URI;
import static org.jboss.remotingjmx.Constants.HTTPS_PROTOCOL;
import static org.jboss.remotingjmx.Constants.HTTP_PROTOCOL;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

/**
 * A holder for utility methods.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:brad.maxwell@redhat.com">Brad Maxwell</a>
 */
public class Util {

    private static Integer DEFAULT_TIMEOUT_SECONDS = 30;

    public static enum Timeout {
        GENERIC("org.jboss.remoting-jmx.timeout"),
        CHANNEL("org.jboss.remoting-jmx.timeout.channel"),
        CONNECTION("org.jboss.remoting-jmx.timeout.connection"),
        VERSIONED_CONNECTION("org.jboss.remoting-jmx.timeout.versioned.connection");

        private String propertyName;

        Timeout(String propertyName) {
            this.propertyName = propertyName;
        }

        public String toString() {
            return propertyName;
        }
    }

    // Prevent instantiation.
    private Util() {
    }

    public static URI convert(final JMXServiceURL serviceUrl) throws IOException {
        String scheme;
        if(serviceUrl.getProtocol().equals(HTTP_PROTOCOL)) {
            scheme = "http";
        } else if(serviceUrl.getProtocol().equals(HTTPS_PROTOCOL)) {
            scheme = "https";
        } else {
            scheme = CONNECTION_PROVIDER_URI;
        }
        String host = serviceUrl.getHost();
        int port = serviceUrl.getPort();

        try {
            return new URI(scheme, null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IOException("Unable to create connection URI", e);
        }
    }

    public static Integer getTimeoutValue(Timeout property, Map<String, ?> environment) {
        // Check for most specific system property first
        Integer timeoutSeconds = Integer.getInteger(property.toString());
        if(timeoutSeconds != null)
            return timeoutSeconds;

        // Check for generic system property 
        timeoutSeconds = Integer.getInteger(Timeout.GENERIC.toString());
        if(timeoutSeconds != null)
            return timeoutSeconds;

        if(environment != null) {
            // Check most specific property in the environment
            timeoutSeconds = getInteger(environment.get(property.toString()));
            if(timeoutSeconds != null)
                return timeoutSeconds;

            // Check property in the environment
            timeoutSeconds = getInteger(environment.get(Timeout.GENERIC.toString()));
            if(timeoutSeconds != null)
                return timeoutSeconds;
        }

        return DEFAULT_TIMEOUT_SECONDS;
    }


    // Return an integer if it is an Integer or can get Integer from String, else null
    public static Integer getInteger(Object object) {
        try {
            if(object instanceof Integer)
                return (Integer) object;

            if(object instanceof String) 
                return Integer.valueOf((String) object); 
        } catch(NumberFormatException  nfe) {
        }
        return null;
    }

}
