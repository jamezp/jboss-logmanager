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

package org.jboss.logmanager.benchmark;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logmanager.AsyncLogger;
import org.jboss.logmanager.ExtLogRecord.FormatStyle;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class LoggerBenchmark {

    protected static final String[] JVM_ARGS = {
            "-Xms768m", "-Xmx768m",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
            "-Dlogging.configuration=" + LoggerBenchmark.class.getResource("/benchmark-logging.properties"),
            "-Dtest.log.dir=" + System.getProperty("test.log.dir"),
    };

    static final String FQCN = LoggerBenchmark.class.getName();

    private final static Logger LOGGER = Logger.getLogger(FQCN);

    private final static AsyncLogger ASYNC_LOGGER = AsyncLogger.getLogger(FQCN);

    private final AtomicLong counter = new AtomicLong();

    @Setup
    public void setup() {
        counter.set(0L);
    }

    @TearDown
    public void tearDown() {
        counter.set(0L);
    }

    @Test
    public void execute() throws Exception {
        final ChainedOptionsBuilder optionsBuilder = new OptionsBuilder()
                .include(".*" + LoggerBenchmark.class.getSimpleName() + ".*")
                .jvmArgsAppend(JVM_ARGS)
                .shouldFailOnError(true);
        final Options options = optionsBuilder.build();

        new Runner(options).run();
    }

    @Benchmark
    public void loggers() {
        LOGGER.log(FQCN, Level.INFO, "this is a test message %d", FormatStyle.PRINTF, new Object[] {counter.incrementAndGet()}, null);
    }

    @Benchmark
    public void asyncLogger() {
        ASYNC_LOGGER.log(FQCN, Level.INFO, "this is a test message %d", FormatStyle.PRINTF, new Object[] {counter.incrementAndGet()}, null);
    }
}
