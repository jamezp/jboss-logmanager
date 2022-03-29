/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

package org.jboss.logmanager;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("unchecked")
public class ContextHolder {
    private final Map<String, Supplier<ErrorManager>> errorManagers;
    private final Map<String, Supplier<Filter>> filters;
    private final Map<String, Supplier<Formatter>> formatters;
    private final Map<String, Supplier<Handler>> handlers;
    private final Map<String, Supplier<Object>> pojos;

    protected ContextHolder() {
        errorManagers = new ConcurrentHashMap<>();
        handlers = new ConcurrentHashMap<>();
        formatters = new ConcurrentHashMap<>();
        filters = new ConcurrentHashMap<>();
        pojos = new ConcurrentHashMap<>();
    }

    public Supplier<ErrorManager> addErrorManager(final String name, final Supplier<ErrorManager> errorManager) {
        return errorManagers.putIfAbsent(name, errorManager);
    }

    public boolean hasErrorManager(final String name) {
        return errorManagers.containsKey(name);
    }

    public <T extends ErrorManager> T getErrorManager(final String name) {
        return (T) errorManagers.get(name).get();
    }

    public Map<String, Supplier<ErrorManager>> getErrorManagers() {
        return Collections.unmodifiableMap(errorManagers);
    }

    public Supplier<Handler> addHandler(final String name, final Supplier<Handler> handler) {
        return handlers.putIfAbsent(name, handler);
    }

    public boolean hasHandler(final String name) {
        return handlers.containsKey(name);
    }

    public <T extends Handler> T getHandler(final String name) {
        return (T) handlers.get(name).get();
    }

    public Map<String, Supplier<Handler>> getHandlers() {
        return Collections.unmodifiableMap(handlers);
    }

    public Supplier<Formatter> addFormatter(final String name, final Supplier<Formatter> formatter) {
        return formatters.putIfAbsent(name, formatter);
    }

    public boolean hasFormatter(final String name) {
        return formatters.containsKey(name);
    }

    public <T extends Formatter> T getFormatter(final String name) {
        return (T) formatters.get(name).get();
    }

    public Map<String, Supplier<Formatter>> getFormatters() {
        return Collections.unmodifiableMap(formatters);
    }

    public Supplier<Filter> addFilter(final String name, final Supplier<Filter> filter) {
        return filters.putIfAbsent(name, filter);
    }

    public boolean hasFilter(final String name) {
        return filters.containsKey(name);
    }

    public <T extends Filter> T getFilter(final String name) {
        return (T) filters.get(name).get();
    }

    public Map<String, Supplier<Filter>> getFilters() {
        return Collections.unmodifiableMap(filters);
    }

    public Supplier<Object> addPojo(final String name, final Supplier<Object> pojo) {
        return pojos.putIfAbsent(name, pojo);
    }

    public boolean hasPojo(final String name) {
        return pojos.containsKey(name);
    }

    public <T extends Filter> T getPojo(final String name) {
        return (T) pojos.get(name).get();
    }

    public Map<String, Supplier<Object>> getPojos() {
        return Collections.unmodifiableMap(pojos);
    }
}
