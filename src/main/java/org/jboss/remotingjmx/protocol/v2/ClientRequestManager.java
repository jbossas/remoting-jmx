/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.remotingjmx.protocol.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager responsible for maintaining the correlation IDs and pending requests.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class ClientRequestManager {

    private int nextCorrelationId = 1;

    /**
     * The in-progress requests awaiting a response.
     */
    private final Map<Integer, VersionedIoFuture> requests = new HashMap<Integer, VersionedIoFuture>();

    /**
     * Get the next correlation ID, returning to the beginning once all integers have been used.
     * <p/>
     * THIS METHOD IS NOT TO BE USED DIRECTLY WHERE A CORRELATION ID NEEDS TO BE RESERVED.
     *
     * @return The next correlationId.
     */
    private synchronized int getNextCorrelationId() {
        int next = nextCorrelationId++;
        // After the maximum integer start back at the beginning.
        if (next < 0) {
            nextCorrelationId = 2;
            next = 1;
        }
        return next;
    }

    /**
     * Reserves a correlation ID by taking the next value and ensuring it is stored in the Map.
     *
     * @return the next reserved correlation ID
     */
    synchronized int reserveNextCorrelationId(VersionedIoFuture future) {
        Integer next = getNextCorrelationId();

        // Not likely but possible to use all IDs and start back at beginning while
        // old request still in progress.
        while (requests.containsKey(next)) {
            next = getNextCorrelationId();
        }
        requests.put(next, future);

        return next;
    }

    synchronized <T> VersionedIoFuture<T> getFuture(int correlationId) {
        // TODO - How to check this?
        return requests.get(correlationId);
    }

    synchronized void releaseCorrelationId(int correlationId) {
        // TODO - Will maybe move to not removing by default and timeout failed requests.
        requests.remove(correlationId);
    }

    synchronized void cancelAllRequests(final IOException io) {
        for (VersionedIoFuture current : requests.values()) {
            current.setException(io);
        }

        requests.clear();
    }

}
