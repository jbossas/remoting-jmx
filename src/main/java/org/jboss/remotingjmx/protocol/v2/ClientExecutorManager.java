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

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper around the executor so it can be passed between the steps.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class ClientExecutorManager {

    private static final String REMOTING_JMX = "remoting-jmx";
    private static final String CLIENT_THREAD = "client-thread-";

    private boolean manageExecutor = false;
    private final Executor executor;

    ClientExecutorManager(final Map<String, ?> environment) {
        if (environment != null && environment.containsKey(Executor.class.getName())) {
            executor = (Executor) environment.get(Executor.class.getName());
        } else {
            executor = Executors.newCachedThreadPool(new ThreadFactory() {

                final ThreadGroup group = new ThreadGroup(REMOTING_JMX);
                final AtomicInteger threadNumber = new AtomicInteger(1);

                public Thread newThread(Runnable r) {
                    return new Thread(group, r, REMOTING_JMX + " " + CLIENT_THREAD + threadNumber.getAndIncrement());
                }
            });
            manageExecutor = true;
        }
    }

    void execute(final Runnable runnable) {
        executor.execute(runnable);
    }

    void close() {
        if (manageExecutor && executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }

}
