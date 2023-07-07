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

package org.jboss.remotingjmx.protocol.v2;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.management.remote.JMXServiceURL;

import org.jboss.remoting3.Channel;
import org.jboss.remotingjmx.Capability;
import org.jboss.remotingjmx.MBeanServerManager;
import org.jboss.remotingjmx.ServerMessageInterceptor;
import org.jboss.remotingjmx.VersionedConnection;

/**
 * The entry point to VersionTwo
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class VersionTwo {

    private VersionTwo() {
    }

    public static byte getVersionIdentifier() {
        return 0x02;
    }

    public static Set<Capability> getCapabilities() {
        return Collections.singleton(Capability.PASS_PARAMETERS);
    }

    public static VersionedConnection getConnection(final Channel channel, final Map<String, ?> environment,
            final JMXServiceURL serviceURL) throws IOException {
        ParameterConnection parameterConnection = new ParameterConnection(channel, environment, serviceURL);

        return parameterConnection.getConnection();
    }

    public static void startServer(final Channel channel, final MBeanServerManager mbeanServerManager, final Executor executor,
            final ServerMessageInterceptor serverMessageInterceptor) throws IOException {
        ParameterProxy proxy = new ParameterProxy(channel, mbeanServerManager, executor, serverMessageInterceptor);
        proxy.start();
    }

}
