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

package org.jboss.remotingjmx.protocol;

import java.io.DataOutputStream;

import org.jboss.remoting3.MessageOutputStream;
import org.xnio.Cancellable;

/**
 * A DataOutputStream implementation to wrap a MessageOutputStream to allow it to be cancelled.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class CancellableDataOutputStream extends DataOutputStream implements Cancellable {

    private final MessageOutputStream mos;

    public CancellableDataOutputStream(MessageOutputStream mos) {
        super(mos);
        this.mos = mos;
    }

    @Override
    public MessageOutputStream cancel() {
        return mos.cancel();
    }

}
