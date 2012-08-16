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

import java.util.Map;

import javax.management.MBeanServerConnection;

/**
 * The interface to be implemented for providing access to the MBeanServers.
 *
 * The result of the method calls may be cached, this is especially true if the returned MBeanServerConnection is actually an
 * MBeanServer.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface MBeanServerLocator {

    /**
     * Obtain the default MBeanServerConnection for when no parameters have been specified.
     *
     * @return The default MBeanServerConnection.
     */
    MBeanServerConnection getDefaultMBeanServer();

    /**
     * Obtain the MBeanServerConnection based on the provided parameters.
     *
     * @param parameters - The connection parameters from the remote client.
     * @return The MBeanServerConnection based on the provided parameters.
     */
    MBeanServerConnection getMBeanServer(final Map<String, String> parameters);

}
