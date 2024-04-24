/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2024 Red Hat, Inc., and individual contributors
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

package org.jboss.logmanager.handlers;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class AsyncTaskHandler {
    private static final AtomicInteger PERMITS = new AtomicInteger(0);

    static <T> CompletionStage<T> supplyAsync(final Supplier<T> supplier) {
        PERMITS.incrementAndGet();
        return CompletableFuture.supplyAsync(supplier, ExecutorHolder.EXECUTOR)
                .whenComplete((v, e) -> PERMITS.decrementAndGet());
    }

    private static class ExecutorHolder {
        static final ExecutorService EXECUTOR;

        static {
            EXECUTOR = Executors.newSingleThreadExecutor(r -> {
                final Thread thread = new Thread(r);
                thread.setName("JBoss Log Manager Handler Task");
                thread.setDaemon(false);
                return thread;
            });
            final Thread shutdownTask = new Thread(() -> {
                long timeout = TimeUnit.MINUTES.toMillis(2L);
                // TODO (jrp) here we need to check the permits and wait until they are complete
                // TODO (jrp) we should likely expose this part further out, possibly in the SuffixRotator, and assign permits for sync tasks too
                // TODO (jrp) timeout?
                while (timeout > 0 && PERMITS.get() > 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100L);
                    } catch (InterruptedException ignore) {
                    }
                    timeout -= 100L;
                }
            });
            shutdownTask.setName("JBoss Log Manager Shutdown Task");
            if (System.getSecurityManager() == null) {
                Runtime.getRuntime().addShutdownHook(shutdownTask);
            } else {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    Runtime.getRuntime().addShutdownHook(shutdownTask);
                    return null;
                });
            }
        }
    }
}
