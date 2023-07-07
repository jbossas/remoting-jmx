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

/**
 * An enumeration to represent capabilities required on opening a new connection.
 *
 * These capabilities could be detected from items within the URL, items specified in the environment or from System properties.
 *
 * Capabilties are only checked on the client side, server side capabilities may be added when required but there is then a risk
 * of breaking connectivity from older clients.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public enum Capability {
    /**
     * The protocol being selected requires that parameters are passed to the server before the connection is actually
     * established.
     */
    PASS_PARAMETERS
}
