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

/**
 * An Interceptor used to wrap the handling of messages on the server side.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface ServerMessageInterceptor {

    /**
     * Called to handle the actual event, gives the Interceptor an opportunity to wrap the call.
     *
     * @param event - The event to run.
     * @throws IOException - If thrown from the event.
     */
    void handleEvent(final Event event) throws IOException;

    public interface Event {
        void run() throws IOException;
    }

}
