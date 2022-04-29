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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.jboss.logmanager.StandardOutputStreams;
import org.jboss.logmanager.configuration.api.ConfigValue;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class PropertyConfigurationWriter {
    private static final String NEW_LINE = System.lineSeparator();
    private final ContextConfiguration contextConfiguration;

    public PropertyConfigurationWriter(final ContextConfiguration contextConfiguration) {
        this.contextConfiguration = contextConfiguration;
    }

    public void writeConfiguration(final OutputStream outputStream, final boolean writeExpressions) throws IOException {
        try (outputStream; final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            final Set<String> implicitHandlers = new HashSet<>();
            final Set<String> implicitFilters = new HashSet<>();
            final Set<String> implicitFormatters = new HashSet<>();
            final Set<String> implicitErrorManagers = new HashSet<>();
            final Map<String, ConfigValue<Logger>> loggers = new LinkedHashMap<>(contextConfiguration.getLoggers());
            writePropertyComment(out, "Additional loggers to configure (the root logger is always configured)");
            writeProperty(out, "loggers", toCsvString(loggers.keySet()));
            final ConfigValue<Logger> rootLogger = loggers.remove("");
            if (rootLogger != null) {
                writeLoggerConfiguration(out, rootLogger, implicitHandlers, implicitFilters);
            }
            for (ConfigValue<Logger> logger : loggers.values()) {
                writeLoggerConfiguration(out, logger, implicitHandlers, implicitFilters);
            }
            final Map<String, ConfigValue<Handler>> handlers = contextConfiguration.getHandlers();
            final Set<String> allHandlerNames = handlers.keySet();
            final List<String> explicitHandlerNames = new ArrayList<>(allHandlerNames);
            explicitHandlerNames.removeAll(implicitHandlers);
            if (!explicitHandlerNames.isEmpty()) {
                writePropertyComment(out, "Additional handlers to configure");
                writeProperty(out, "handlers", toCsvString(explicitHandlerNames));
                out.write(NEW_LINE);
            }
            for (ConfigValue<Handler> handler : handlers.values()) {
                writeHandlerConfiguration(out, handler, implicitHandlers, implicitFilters,
                        implicitFormatters, implicitErrorManagers);
            }
            final Map<String, ConfigValue<Filter>> filters = contextConfiguration.getFilters();
            final List<String> explicitFilterNames = new ArrayList<>(filters.keySet());
            explicitFilterNames.removeAll(implicitFilters);
            if (!explicitFilterNames.isEmpty()) {
                writePropertyComment(out, "Additional filters to configure");
                writeProperty(out, "filters", toCsvString(explicitFilterNames));
                out.write(NEW_LINE);
            }
            for (ConfigValue<Filter> filter : filters.values()) {
                writeFilterConfiguration(out, filter);
            }
            final Map<String, ConfigValue<Formatter>> formatters = contextConfiguration.getFormatters();
            final ArrayList<String> explicitFormatterNames = new ArrayList<>(formatters.keySet());
            explicitFormatterNames.removeAll(implicitFormatters);
            if (!explicitFormatterNames.isEmpty()) {
                writePropertyComment(out, "Additional formatters to configure");
                writeProperty(out, "formatters", toCsvString(explicitFormatterNames));
                out.write(NEW_LINE);
            }
            for (ConfigValue<Formatter> formatter : formatters.values()) {
                writeFormatterConfiguration(out, formatter);
            }
            final Map<String, ConfigValue<ErrorManager>> errorManagers = contextConfiguration.getErrorManagers();
            final ArrayList<String> explicitErrorManagerNames = new ArrayList<>(errorManagers.keySet());
            explicitErrorManagerNames.removeAll(implicitErrorManagers);
            if (!explicitErrorManagerNames.isEmpty()) {
                writePropertyComment(out, "Additional errorManagers to configure");
                writeProperty(out, "errorManagers", toCsvString(explicitErrorManagerNames));
                out.write(NEW_LINE);
            }
            for (ConfigValue<ErrorManager> errorManager : errorManagers.values()) {
                writeErrorManagerConfiguration(out, errorManager);
            }

            // Write POJO configurations
            final Map<String, ConfigValue<Object>> objects = contextConfiguration.getObjects();
            if (!objects.isEmpty()) {
                writePropertyComment(out, "POJOs to configure");
                writeProperty(out, "pojos", toCsvString(objects.keySet()));
                for (ConfigValue<Object> object : objects.values()) {
                    writePojoConfiguration(out, object);
                }
            }

            out.flush();
            outputStream.flush();
        }
    }

    private void writeLoggerConfiguration(final Writer out, final ConfigValue<Logger> logger,
                                          final Set<String> implicitHandlers, final Set<String> implicitFilters)
            throws IOException {
        if (logger != null) {
            out.write(NEW_LINE);
            final String name = logger.name();
            final String prefix = name.isEmpty() ? "logger." : "logger." + name + ".";
            final String level = logger.property("level");
            if (level != null) {
                writeProperty(out, prefix, "level", level);
            }
            final String filterName = logger.property("filter");
            if (filterName != null) {
                writeProperty(out, prefix, "filter", filterName);
                implicitFilters.add(filterName);
            }
            final String useParentHandlers = logger.property("useParentHandlers");
            if (useParentHandlers != null) {
                writeProperty(out, prefix, "useParentHandlers", useParentHandlers);
            }
            final List<String> handlerNames = new ArrayList<>();
            if (logger.propertyNames().contains("handlers")) {
                for (String handlerName : logger.property("handlers").split("\\s*,\\s*")) {
                    if (contextConfiguration.hasHandler(handlerName)) {
                        implicitHandlers.add(handlerName);
                        handlerNames.add(handlerName);
                    } else {
                        printError("Handler %s is not defined and will not be written to the configuration for logger %s%n", handlerName, (name.isEmpty() ? "ROOT" : name));
                    }
                }
                if (!handlerNames.isEmpty()) {
                    writeProperty(out, prefix, "handlers", toCsvString(handlerNames));
                }
            }
        }
    }

    private void writeHandlerConfiguration(final Writer out, final ConfigValue<Handler> handler,
                                           final Set<String> implicitHandlers, final Set<String> implicitFilters,
                                           final Set<String> implicitFormatters,
                                           final Set<String> implicitErrorManagers) throws IOException {
        if (handler != null) {
            out.write(NEW_LINE);
            final String name = handler.name();
            final String prefix = "handler." + name + ".";
            final String className = handler.className();
            writeProperty(out, "handler.", name, className);
            final String moduleName = handler.moduleName();
            if (moduleName != null) {
                writeProperty(out, prefix, "module", moduleName);
            }
            final String level = handler.property("level");
            if (level != null) {
                writeProperty(out, prefix, "level", level);
            }
            final String encoding = handler.property("encoding");
            if (encoding != null) {
                writeProperty(out, prefix, "encoding", encoding);
            }
            final String filter = handler.property("filter");
            if (filter != null) {
                writeProperty(out, prefix, "filter", filter);
                implicitFilters.add(filter);
            }
            final String formatterName = handler.property("formatter");
            if (formatterName != null) {
                // Make sure the formatter exists
                if (contextConfiguration.hasFormatter(formatterName)) {
                    writeProperty(out, prefix, "formatter", formatterName);
                    implicitFormatters.add(formatterName);
                } else {
                    printError("Formatter %s is not defined and will not be written to the configuration for handler %s%n", formatterName, name);
                }
            }
            final String errorManagerName = handler.property("errorManager");
            if (errorManagerName != null) {
                // Make sure the error manager exists
                if (contextConfiguration.hasErrorManager(errorManagerName)) {
                    writeProperty(out, prefix, "errorManager", errorManagerName);
                    implicitErrorManagers.add(errorManagerName);
                } else {
                    printError("Error manager %s is not defined and will not be written to the configuration for handler %s%n", errorManagerName, name);
                }
            }
            final List<String> handlerNames = new ArrayList<>();
            if (handler.propertyNames().contains("handlers")) {
                for (String handlerName : handler.property("handlers").split("\\s*,\\s*")) {
                    if (contextConfiguration.hasHandler(handlerName)) {
                        implicitHandlers.add(handlerName);
                        handlerNames.add(handlerName);
                    } else {
                        printError("Handler %s is not defined and will not be written to the configuration for handler %s%n", handlerName, name);
                    }
                }
                if (!handlerNames.isEmpty()) {
                    writeProperty(out, prefix, "handlers", toCsvString(handlerNames));
                }
            }
            final Set<String> postConfigurationMethods = handler.postConstructMethods();
            if (!postConfigurationMethods.isEmpty()) {
                writeProperty(out, prefix, "postConfiguration", toCsvString(postConfigurationMethods));
            }
            writeProperties(out, prefix, handler, Set.of("level", "encoding", "filter", "errorManager", "formatter", "handlers"));
        }
    }

    private static void writeFilterConfiguration(final Writer out, final ConfigValue<Filter> filter)
            throws IOException {
        if (filter != null) {
            out.write(NEW_LINE);
            final String name = filter.name();
            final String prefix = "filter." + name + ".";
            writeProperty(out, "filter.", name, filter.className());
            final String moduleName = filter.moduleName();
            if (moduleName != null) {
                writeProperty(out, prefix, "module", moduleName);
            }
            final Set<String> postConfigurationMethods = filter.postConstructMethods();
            if (!postConfigurationMethods.isEmpty()) {
                writeProperty(out, prefix, "postConfiguration", toCsvString(postConfigurationMethods));
            }
            writeProperties(out, prefix, filter);
        }
    }

    private static void writeFormatterConfiguration(final Writer out, final ConfigValue<Formatter> formatter)
            throws IOException {
        if (formatter != null) {
            out.write(NEW_LINE);
            final String name = formatter.name();
            final String prefix = "formatter." + name + ".";
            final String className = formatter.className();
            writeProperty(out, "formatter.", name, className);
            final String moduleName = formatter.moduleName();
            if (moduleName != null) {
                writeProperty(out, prefix, "module", moduleName);
            }
            final Set<String> postConfigurationMethods = formatter.postConstructMethods();
            if (!postConfigurationMethods.isEmpty()) {
                writeProperty(out, prefix, "postConfiguration", toCsvString(postConfigurationMethods));
            }
            writeProperties(out, prefix, formatter);
        }
    }

    private static void writeErrorManagerConfiguration(final Writer out, final ConfigValue<ErrorManager> errorManager)
            throws IOException {
        if (errorManager != null) {
            out.write(NEW_LINE);
            final String name = errorManager.name();
            final String prefix = "errorManager." + name + ".";
            final String className = errorManager.className();
            writeProperty(out, "errorManager.", name, className);
            final String moduleName = errorManager.moduleName();
            if (moduleName != null) {
                writeProperty(out, prefix, "module", moduleName);
            }
            final Set<String> postConfigurationMethods = errorManager.postConstructMethods();
            if (!postConfigurationMethods.isEmpty()) {
                writeProperty(out, prefix, "postConfiguration", toCsvString(postConfigurationMethods));
            }
            writeProperties(out, prefix, errorManager);
        }
    }

    private static void writePojoConfiguration(final Writer out, final ConfigValue<Object> pojo) throws IOException {
        if (pojo != null) {
            out.write(NEW_LINE);
            final String name = pojo.name();
            final String prefix = "pojo." + name + ".";
            final String className = pojo.className();
            writeProperty(out, "pojo.", name, className);
            final String moduleName = pojo.moduleName();
            if (moduleName != null) {
                writeProperty(out, prefix, "module", moduleName);
            }
            final Set<String> postConfigurationMethods = pojo.postConstructMethods();
            if (!postConfigurationMethods.isEmpty()) {
                writeProperty(out, prefix, "postConfiguration", toCsvString(postConfigurationMethods));
            }
            writeProperties(out, prefix, pojo);
        }
    }

    /**
     * Writes a comment to the print stream. Prepends the comment with a {@code #}.
     *
     * @param out     the print stream to write to.
     * @param comment the comment to write.
     */
    private static void writePropertyComment(final Writer out, final String comment) throws IOException {
        out.write(NEW_LINE);
        out.write("# ");
        out.write(comment);
        out.write(NEW_LINE);
    }

    /**
     * Writes a property to the print stream.
     *
     * @param out   the print stream to write to.
     * @param name  the name of the property.
     * @param value the value of the property.
     */
    private static void writeProperty(final Writer out, final String name, final String value) throws IOException {
        writeProperty(out, null, name, value);
    }

    /**
     * Writes a property to the print stream.
     *
     * @param out    the print stream to write to.
     * @param prefix the prefix for the name or {@code null} to use no prefix.
     * @param name   the name of the property.
     * @param value  the value of the property.
     */
    private static void writeProperty(final Writer out, final String prefix, final String name, final String value)
            throws IOException {
        if (prefix == null) {
            writeKey(out, name);
        } else {
            writeKey(out, String.format("%s%s", prefix, name));
        }
        writeValue(out, value);
        out.write(NEW_LINE);
    }

    private static void writeProperties(final Writer out, final String prefix, final ConfigValue<?> configValue)
            throws IOException {
        writeProperties(out, prefix, configValue, Collections.emptySet());
    }


    /**
     * Writes a collection of properties to the print stream.
     *
     * @param out         the print stream to write to.
     * @param prefix      the prefix for the name or {@code null} to use no prefix.
     * @param configValue the configuration to extract the property value from.
     */
    private static void writeProperties(final Writer out, final String prefix,
                                        final ConfigValue<?> configValue, final Set<String> ignoredProperties)
            throws IOException {
        final Set<String> names = configValue.propertyNames();
        if (!names.isEmpty()) {
            final Set<String> ctorProps = configValue.constructorPropertyNames();
            if (prefix == null) {
                writeProperty(out, "properties", toCsvString(names, ignoredProperties));
                if (!ctorProps.isEmpty()) {
                    writeProperty(out, "constructorProperties", toCsvString(ctorProps, ignoredProperties));
                }
                for (String name : ctorProps) {
                    writeProperty(out, name, configValue.constructorProperty(name));
                }
                for (String name : names) {
                    writeProperty(out, name, configValue.property(name));
                }
            } else {
                writeProperty(out, prefix, "properties", toCsvString(names, ignoredProperties));
                if (!ctorProps.isEmpty()) {
                    writeProperty(out, prefix, "constructorProperties", toCsvString(ctorProps, ignoredProperties));
                }
                for (String name : ctorProps) {
                    writeProperty(out, prefix, name, configValue.constructorProperty(name));
                }
                for (String name : names) {
                    writeProperty(out, prefix, name, configValue.property(name));
                }
            }
        }
    }

    /**
     * Parses the list and creates a comma delimited string of the names.
     * <p/>
     * <b>Notes:</b> empty names are ignored.
     *
     * @param names the names to process.
     *
     * @return a comma delimited list of the names.
     */
    private static String toCsvString(final Collection<String> names) {
        return toCsvString(names, Collections.emptyList());
    }

    /**
     * Parses the list and creates a comma delimited string of the names.
     * <p/>
     * <b>Notes:</b> empty names are ignored.
     *
     * @param names the names to process.
     *
     * @return a comma delimited list of the names.
     */
    private static String toCsvString(final Collection<String> names, final Collection<String> ignoredValues) {
        final StringBuilder result = new StringBuilder(1024);
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
            final String name = iterator.next();
            if (ignoredValues.contains(name)) continue;
            // No need to write empty names
            if (!name.isEmpty()) {
                result.append(name);
                if (iterator.hasNext()) {
                    result.append(",");
                }
            }
        }
        return result.toString();
    }

    private static void writeValue(final Appendable out, final String value) throws IOException {
        writeSanitized(out, value, false);
    }

    private static void writeKey(final Appendable out, final String key) throws IOException {
        writeSanitized(out, key, true);
        out.append('=');
    }

    private static void writeSanitized(final Appendable out, final String string, final boolean escapeSpaces)
            throws IOException {
        for (int x = 0; x < string.length(); x++) {
            final char c = string.charAt(x);
            switch (c) {
                case ' ':
                    if (x == 0 || escapeSpaces)
                        out.append('\\');
                    out.append(c);
                    break;
                case '\t':
                    out.append('\\').append('t');
                    break;
                case '\n':
                    out.append('\\').append('n');
                    break;
                case '\r':
                    out.append('\\').append('r');
                    break;
                case '\f':
                    out.append('\\').append('f');
                    break;
                case '\\':
                case '=':
                case ':':
                case '#':
                case '!':
                    out.append('\\').append(c);
                    break;
                default:
                    out.append(c);
            }
        }
    }

    /**
     * Prints the message to stderr.
     *
     * @param format the format of the message
     * @param args   the format arguments
     */
    private static void printError(final String format, final Object... args) {
        StandardOutputStreams.printError(format, args);
    }
}
