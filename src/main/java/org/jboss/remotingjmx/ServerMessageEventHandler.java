/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.remoting3.Channel;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @deprecated Experimental, may change
 */
@Deprecated
public abstract class ServerMessageEventHandler {

    /**
     * @deprecated Experimental, may change
     */
    @Deprecated
    protected ServerMessageEventHandler(Channel channel) {
        // No need to store the channel, leave that to the subclasses
    }

    /**
     * Called before handling a protocol message on the server.
     *
     * @param channel the channel handling the protocol message
     * @deprecated Experimental, may change
     */
    @Deprecated
    public void beforeEvent() {
    }

    /**
     * Called after handling a protocol message on the server completed either normally or throwing an error
     *
     * @param channel the channel handling the protocol message
     * @param thrown the exception thrown if handling the message resulted in an error
     * @deprecated Experimental, may change
     */
    @Deprecated
    public void afterEvent(Throwable thrown) {
    }
}
