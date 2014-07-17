/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.logmanager;

import java.util.InputMismatchException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class AsyncLoggerService {

    static final class Holder {
        static final AsyncLoggerService INSTANCE = new AsyncLoggerService();
    }

    public static AsyncLoggerService getInstance() {
        return Holder.INSTANCE;
    }

    private final AtomicBoolean started;
    private final BlockingDeque<Runnable> queue;
    private final Thread thread;

    private AsyncLoggerService() {
        started = new AtomicBoolean(false);
        queue = new LinkedBlockingDeque<>(500);
        thread = Executors.defaultThreadFactory().newThread(new AsyncTask());
        thread.setDaemon(true);
    }

    public void submit(final Runnable task) {
        try {
            if (!started.get()) {
                if (started.compareAndSet(false, true)) {
                    thread.start();
                }
            }
            queue.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private class AsyncTask implements Runnable {

        private final BlockingDeque<Runnable> queue = AsyncLoggerService.this.queue;

        @Override
        public void run() {
            boolean interrupted = false;
            try {
                while (!interrupted) {
                    try {
                        final Runnable runnable = queue.take();
                        runnable.run();
                    } catch (InputMismatchException e) {
                        interrupted = true;
                    }
                }
            } catch (Exception e) {
                // TODO (jrp) report the error
                e.printStackTrace();
            } catch (Throwable t) {
                // ignore :-/
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
