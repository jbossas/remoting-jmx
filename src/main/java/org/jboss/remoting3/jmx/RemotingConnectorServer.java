/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Registration;
import org.xnio.OptionMap;

/**
 * A JMXConnectorServer to handle the server side of the lifecycle relating to making
 * the provided MBeanServer accessible using JBoss Remoting.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RemotingConnectorServer extends JMXConnectorServer {

    private static final Logger log = Logger.getLogger(RemotingConnectorServer.class);

    /**
     * The default channel name for JMX
     */
    private static final String JMX_CHANNEL_NAME = "jmx";
    // TODO - We may need this to be configurable to expose multiple MBeanServers
    // TODO - Either that or selection of MBean server is on marshalled message but not sure if that is correct.

    private boolean started = false;
    private boolean stopped = false;

    /**
     * The Remoting Endpoint this ConnectorServer will register against when it is started.
     */
    private Endpoint endpoint;
    private Registration registration;

    public RemotingConnectorServer(final MBeanServer mbeanServer, final Endpoint endpoint) {
        super(mbeanServer);
        this.endpoint = endpoint;
    }

    /*
     * Methods from JMXConnectorServerMBean
     */

    public void start() throws IOException {
        log.info("start()");
        if (stopped) {
            throw new IOException("Unable to start connector as already stopped.");
        }

        // If this ConnectorServer has already started just return.
        if (started) {
            return;
        }

        log.info("Registering service");
        registration = endpoint.registerService(JMX_CHANNEL_NAME, new ChannelOpenListener(), OptionMap.EMPTY);
        started = true;
    }

    public void stop() throws IOException {
        // If successfully stopped just return.
        if (stopped) {
            return;
        }

        try {
            if (started) {
                // TODO - How to correctly handle existing clients and notify them to disconnect?
                registration.close();
            }
        } finally {
            // Even if the connector server had not been started calling stop permenantly
            // disables the connector server.
            endpoint = null;
            registration = null;
            stopped = true;
        }

    }

    public boolean isActive() {
        // The connector server is active when it has been started but not stopped.
        return started && !stopped;
    }

    public JMXServiceURL getAddress() {
        // Using Remoting we don't have direct access to the address so for now
        // assume there isn't one available and return null.
        return null;
    }

    public Map<String, ?> getAttributes() {
        // TODO - What attributes are there to return?
        return Collections.emptyMap();
    }

    /*
     *  Handlers and Recievers
     */

    /**
     * The listener to handle the opening of the channel from remote clients.
     */
    private class ChannelOpenListener implements OpenListener {

        public void channelOpened(Channel channel) {
            log.info("Channel Opened");
        }

        public void registrationTerminated() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

}
