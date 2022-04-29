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

package org.jboss.logmanager.configuration;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.jboss.logmanager.configuration.api.ConfigValue;

/**
 * A configuration which can be stored on a {@linkplain org.jboss.logmanager.LogContext log context} to store information
 * about the configured error managers, handlers, filters, formatters and objects that might be associated with a
 * configured object.
 * <p>
 * The {@link #addObject(String, ConfigValue)} can be used to allow objects to be set when configuring error managers,
 * handlers, filters and formatters.
 * </p>
 * <p>
 * {@link ConfigValue}'s are used to return information and the value of the configuration object.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) do we need to have the defined loggers?
// TODO (jrp) we need a way to clear the configuration when the context is closed
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class ContextConfiguration {
    public static final org.jboss.logmanager.Logger.AttachmentKey<ContextConfiguration> CONTEXT_CONFIGURATION_KEY = new org.jboss.logmanager.Logger.AttachmentKey<>();

    private final Map<String, ConfigValue<ErrorManager>> errorManagers;
    private final Map<String, ConfigValue<Filter>> filters;
    private final Map<String, ConfigValue<Formatter>> formatters;
    private final Map<String, ConfigValue<Handler>> handlers;
    private final Map<String, ConfigValue<Logger>> loggers;
    private final Map<String, ConfigValue<Object>> objects;

    /**
     * Creates a new context configuration.
     */
    public ContextConfiguration() {
        // TODO (jrp) the order may matter when writing it out, so ideally we'd use an known order
        errorManagers = new ConcurrentSkipListMap<>();
        handlers = new ConcurrentSkipListMap<>();
        formatters = new ConcurrentSkipListMap<>();
        filters = new ConcurrentSkipListMap<>();
        loggers = new ConcurrentSkipListMap<>();
        objects = new ConcurrentSkipListMap<>();
    }

    /**
     * Adds an error manager to the context configuration.
     *
     * @param name         the name for the error manager
     * @param errorManager the error manager to add
     *
     * @return the previous error manager associated with the name or {@code null} if one did not exist
     */
    public ConfigValue<ErrorManager> addErrorManager(final String name,
                                                     final ConfigValue<ErrorManager> errorManager) {
        if (errorManager == null) {
            return removeErrorManager(name);
        }
        return errorManagers.putIfAbsent(Objects.requireNonNull(name, "The name cannot be null"), errorManager);
    }

    /**
     * Removes the error manager from the context configuration.
     *
     * @param name the name of the error manager
     *
     * @return the error manager removed or {@code null} if the error manager did not exist
     */
    public ConfigValue<ErrorManager> removeErrorManager(final String name) {
        return errorManagers.remove(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Checks if the error manager exists with the name provided.
     *
     * @param name the name for the error manager
     *
     * @return {@code true} if the error manager exists in this context, otherwise {@code false}
     */
    public boolean hasErrorManager(final String name) {
        return errorManagers.containsKey(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Gets the error manager if it exists.
     *
     * @param name the name of the error manager
     *
     * @return the error manager or {@code null} if the error manager does not exist
     */
    public ConfigValue<ErrorManager> getErrorManager(final String name) {
        if (errorManagers.containsKey(Objects.requireNonNull(name, "The name cannot be null"))) {
            return errorManagers.get(name);
        }
        return null;
    }

    /**
     * Returns an unmodifiable map of the error managers and the configuration values used to create them.
     *
     * @return an unmodified map of the error managers
     */
    public Map<String, ConfigValue<ErrorManager>> getErrorManagers() {
        return Collections.unmodifiableMap(errorManagers);
    }

    /**
     * Adds a handler to the context configuration.
     *
     * @param name    the name for the handler
     * @param handler the handler to add
     *
     * @return the previous handler associated with the name or {@code null} if one did not exist
     */
    public ConfigValue<Handler> addHandler(final String name, final ConfigValue<Handler> handler) {
        if (handler == null) {
            return removeHandler(name);
        }
        return handlers.putIfAbsent(Objects.requireNonNull(name, "The name cannot be null"), handler);
    }

    /**
     * Removes the handler from the context configuration.
     *
     * @param name the name of the handler
     *
     * @return the handler removed or {@code null} if the handler did not exist
     */
    public ConfigValue<Handler> removeHandler(final String name) {
        return handlers.remove(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Checks if the handler exists with the name provided.
     *
     * @param name the name for the handler
     *
     * @return {@code true} if the handler exists in this context, otherwise {@code false}
     */
    public boolean hasHandler(final String name) {
        return handlers.containsKey(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Gets the handler if it exists.
     *
     * @param name the name of the handler
     *
     * @return the handler or {@code null} if the handler does not exist
     */
    public Handler getHandler(final String name) {
        if (handlers.containsKey(Objects.requireNonNull(name, "The name cannot be null"))) {
            return handlers.get(name).value();
        }
        return null;
    }

    /**
     * Returns an unmodifiable map of the handlers and the configuration values used to create them.
     *
     * @return an unmodified map of the handlers
     */
    public Map<String, ConfigValue<Handler>> getHandlers() {
        return Collections.unmodifiableMap(handlers);
    }

    /**
     * Adds a formatter to the context configuration.
     *
     * @param name      the name for the formatter
     * @param formatter the formatter to add
     *
     * @return the previous formatter associated with the name or {@code null} if one did not exist
     */
    public ConfigValue<Formatter> addFormatter(final String name, final ConfigValue<Formatter> formatter) {
        if (formatter == null) {
            return removeFormatter(name);
        }
        return formatters.putIfAbsent(Objects.requireNonNull(name, "The name cannot be null"), formatter);
    }

    /**
     * Removes the formatter from the context configuration.
     *
     * @param name the name of the formatter
     *
     * @return the formatter removed or {@code null} if the formatter did not exist
     */
    public ConfigValue<Formatter> removeFormatter(final String name) {
        return formatters.remove(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Checks if the formatter exists with the name provided.
     *
     * @param name the name for the formatter
     *
     * @return {@code true} if the formatter exists in this context, otherwise {@code false}
     */
    public boolean hasFormatter(final String name) {
        return formatters.containsKey(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Gets the formatter if it exists.
     *
     * @param name the name of the formatter
     *
     * @return the formatter or {@code null} if the formatter does not exist
     */
    public Formatter getFormatter(final String name) {
        if (formatters.containsKey(Objects.requireNonNull(name, "The name cannot be null"))) {
            return formatters.get(name).value();
        }
        return null;
    }

    /**
     * Returns an unmodifiable map of the formatters and the configuration values used to create them.
     *
     * @return an unmodified map of the formatters
     */
    public Map<String, ConfigValue<Formatter>> getFormatters() {
        return Collections.unmodifiableMap(formatters);
    }

    /**
     * Adds a filter to the context configuration.
     *
     * @param name   the name for the filter
     * @param filter the filter to add
     *
     * @return the previous filter associated with the name or {@code null} if one did not exist
     */
    public ConfigValue<Filter> addFilter(final String name, final ConfigValue<Filter> filter) {
        if (filter == null) {
            return removeFilter(name);
        }
        return filters.putIfAbsent(Objects.requireNonNull(name, "The name cannot be null"), filter);
    }

    /**
     * Removes the filter from the context configuration.
     *
     * @param name the name of the filter
     *
     * @return the filter removed or {@code null} if the filter did not exist
     */
    public ConfigValue<Filter> removeFilter(final String name) {
        return filters.remove(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Checks if the filter exists with the name provided.
     *
     * @param name the name for the filter
     *
     * @return {@code true} if the filter exists in this context, otherwise {@code false}
     */
    public boolean hasFilter(final String name) {
        return filters.containsKey(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Gets the filter if it exists.
     *
     * @param name the name of the filter
     *
     * @return the filer or {@code null} if the filter does not exist
     */
    public Filter getFilter(final String name) {
        if (filters.containsKey(Objects.requireNonNull(name, "The name cannot be null"))) {
            return filters.get(name).value();
        }
        return null;
    }

    /**
     * Returns an unmodifiable map of the filters and the configuration values used to create them.
     *
     * @return an unmodified map of the filters
     */
    public Map<String, ConfigValue<Filter>> getFilters() {
        return Collections.unmodifiableMap(filters);
    }


    // TODO (jrp) update the documentation and should it just be addLogger(String name)?

    /**
     * Adds a configured logger to the context.
     *
     * @param name   the name for the configuration object
     * @param object the configuration object to add
     *
     * @return the previous configuration object associated with the name or {@code null} if one did not exist
     */
    public ConfigValue<Logger> addLogger(final String name, final ConfigValue<Logger> logger) {
        if (logger == null) {
            return removeLogger(name);
        }
        return loggers.putIfAbsent(Objects.requireNonNull(name, "The name cannot be null"), logger);
    }

    /**
     * Removes the configuration object from the context configuration.
     *
     * @param name the name of the configuration object
     *
     * @return the configuration object removed or {@code null} if the configuration object did not exist
     */
    public ConfigValue<Logger> removeLogger(final String name) {
        return loggers.remove(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Checks if the configuration object exists with the name provided.
     *
     * @param name the name for the configuration object
     *
     * @return {@code true} if the configuration object exists in this context, otherwise {@code false}
     */
    public boolean hasLogger(final String name) {
        return loggers.containsKey(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Gets the configuration object if it exists.
     *
     * @param name the name of the configuration object
     *
     * @return the configuration object or {@code null} if the configuration object does not exist
     */
    public Logger getLogger(final String name) {
        if (loggers.containsKey(Objects.requireNonNull(name, "The name cannot be null"))) {
            return loggers.get(name).value();
        }
        return null;
    }

    /**
     * Returns an unmodifiable map of the loggers and the configuration values used to create them.
     *
     * @return an unmodified map of the loggers
     */
    public Map<String, ConfigValue<Logger>> getLoggers() {
        return Collections.unmodifiableMap(loggers);
    }

    /**
     * Adds an object that can be used as a configuration property for another configuration type. This is used for
     * cases when an object cannot simply be converted from a string.
     *
     * @param name   the name for the configuration object
     * @param object the configuration object to add
     *
     * @return the previous configuration object associated with the name or {@code null} if one did not exist
     */
    public ConfigValue<Object> addObject(final String name, final ConfigValue<Object> object) {
        if (object == null) {
            return removeObject(name);
        }
        return objects.putIfAbsent(Objects.requireNonNull(name, "The name cannot be null"), object);
    }

    /**
     * Removes the configuration object from the context configuration.
     *
     * @param name the name of the configuration object
     *
     * @return the configuration object removed or {@code null} if the configuration object did not exist
     */
    public ConfigValue<Object> removeObject(final String name) {
        return objects.remove(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Checks if the configuration object exists with the name provided.
     *
     * @param name the name for the configuration object
     *
     * @return {@code true} if the configuration object exists in this context, otherwise {@code false}
     */
    public boolean hasObject(final String name) {
        return objects.containsKey(Objects.requireNonNull(name, "The name cannot be null"));
    }

    /**
     * Gets the configuration object if it exists.
     *
     * @param name the name of the configuration object
     *
     * @return the configuration object or {@code null} if the configuration object does not exist
     */
    public Object getObject(final String name) {
        if (objects.containsKey(Objects.requireNonNull(name, "The name cannot be null"))) {
            return objects.get(name).value();
        }
        return null;
    }

    /**
     * Returns an unmodifiable map of the configuration objects and the configuration values used to create them.
     *
     * @return an unmodified map of the configuration objects
     */
    public Map<String, ConfigValue<Object>> getObjects() {
        return Collections.unmodifiableMap(objects);
    }
}
