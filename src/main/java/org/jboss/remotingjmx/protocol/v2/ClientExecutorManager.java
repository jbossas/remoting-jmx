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

import java.security.AccessController;
import java.security.PrivilegedAction;
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
    private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);

    private boolean manageExecutor = false;
    private final Executor executor;

    static final ThreadGroup THREAD_GROUP;
    static {
        THREAD_GROUP = AccessController.doPrivileged(new PrivilegedAction<ThreadGroup>() {
            public ThreadGroup run() {
                ThreadGroup t = Thread.currentThread().getThreadGroup();
                while (t.getParent() != null) t = t.getParent();
                return t;
            }
        });
    }

    ClientExecutorManager(final Map<String, ?> environment) {
        if (environment != null && environment.containsKey(Executor.class.getName())) {
            executor = (Executor) environment.get(Executor.class.getName());
        } else {
            executor = Executors.newCachedThreadPool(new ThreadFactory() {

                public Thread newThread(Runnable r) {
                    return new Thread(THREAD_GROUP, r, REMOTING_JMX + " " + CLIENT_THREAD + THREAD_NUMBER.getAndIncrement());
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
