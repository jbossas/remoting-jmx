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
