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
     * @return The MBeanServerConnection based on the provided parameters, or null if no MBeanServerConnection selected.
     */
    MBeanServerConnection getMBeanServer(final Map<String, String> parameters);

}
