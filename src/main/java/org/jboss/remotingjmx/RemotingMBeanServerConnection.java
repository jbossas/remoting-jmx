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

import javax.management.MBeanServerConnection;

import org.jboss.remoting3.Connection;

/**
 * An extension of the MBeanServerConnection to also expose the underlying Remoting Connection.
 *
 * WARNING - This is an internal API for use by the CLI / jConsole integration only.
 *
 * This interface may be changed unexpectedly between releases.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface RemotingMBeanServerConnection extends MBeanServerConnection {

    Connection getConnection();

}
