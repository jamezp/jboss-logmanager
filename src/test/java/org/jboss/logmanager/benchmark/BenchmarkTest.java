/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2015 Red Hat, Inc., and individual contributors
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

package org.jboss.logmanager.benchmark;

import java.util.ArrayList;
import java.util.Collection;

import org.jboss.logmanager.LogManager;
import org.junit.Test;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BenchmarkTest {

    protected static final String[] JVM_ARGS = {
            "-Xms768m", "-Xmx768m",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
    };

    @Test
    public void runBenchmarks() throws RunnerException {
        final Collection<RunResult> results = new ArrayList<>();
        results.addAll(new Runner(createBuilder(DefaultLoggerBenchmark.class).build()).run());


        ChainedOptionsBuilder optionsBuilder = createBuilder(ThreadLocalFilterOnNoFilterBenchmark.class)
                .jvmArgsAppend("-D" + LogManager.PER_THREAD_LOG_FILTER_KEY + "=true");
        results.addAll(new Runner(optionsBuilder.build()).run());


        optionsBuilder = createBuilder(ThreadLocalFilterLoggerBenchmark.class)
                .jvmArgsAppend("-D" + LogManager.PER_THREAD_LOG_FILTER_KEY + "=true");
        results.addAll(new Runner(optionsBuilder.build()).run());


        // Print the results
        System.out.println();
        System.out.println("Aggregate Benchmark Results:");
        ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out).writeOut(results);
    }

    private ChainedOptionsBuilder createBuilder(final Class<?> c) {
        return new OptionsBuilder()
                .include(c.getSimpleName())
                .jvmArgsPrepend(JVM_ARGS)
                .shouldFailOnError(true);
    }
}
