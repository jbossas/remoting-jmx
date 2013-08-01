/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.io.IOException;

import org.jboss.remoting3.Channel;

/**
 * A default implementation of {@link ServerMessageInterceptorFactory} so we can avoid null checks all over the place.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class DefaultServerInterceptorFactory implements ServerMessageInterceptorFactory {

    private static final ServerMessageInterceptor INTERCEPTOR_INSTANCE = new ServerMessageInterceptor() {

        @Override
        public void handleEvent(Event event) throws IOException {
            event.run();
        }
    };

    static final ServerMessageInterceptorFactory FACTORY_INSTANCE = new DefaultServerInterceptorFactory();

    private DefaultServerInterceptorFactory() {
    }

    @Override
    public ServerMessageInterceptor create(Channel channel) {
        return INTERCEPTOR_INSTANCE;
    }

}
