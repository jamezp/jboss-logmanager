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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtLogRecord.FormatStyle;
import org.jboss.logmanager.Logger.AttachmentKey;

/**
 * An actual logger instance.  This is the end-user interface into the logging system.
 */
@SuppressWarnings({"SerializableClassWithUnconstructableAncestor"})
public final class AsyncLogger extends java.util.logging.Logger implements Serializable {

    private static final long serialVersionUID = 5093333069125075416L;

    /**
     * The named logger tree node.
     */
    private final Logger delegate;

    private final AsyncLoggerService service = AsyncLoggerService.getInstance();

    private static final String LOGGER_CLASS_NAME = AsyncLogger.class.getName();
    private static final AttachmentKey<AsyncLogger> PARENT_KEY = new AttachmentKey<>();

    /**
     * Static logger factory method which returns a JBoss LogManager logger.
     *
     * @param name the logger name
     *
     * @return the logger
     */
    public static AsyncLogger getLogger(final String name) {
        try {
            // call through j.u.l.Logger so that primordial configuration is set up
            return new AsyncLogger(Logger.getLogger(name), name);
        } catch (ClassCastException e) {
            throw new IllegalStateException("The LogManager was not properly installed (you must set the \"java.util.logging.manager\" system property to \"" + LogManager.class.getName() + "\")");
        }
    }

    /**
     * Static logger factory method which returns a JBoss LogManager logger.
     *
     * @param name       the logger name
     * @param bundleName the bundle name
     *
     * @return the logger
     */
    public static AsyncLogger getLogger(final String name, final String bundleName) {
        try {
            // call through j.u.l.Logger so that primordial configuration is set up
            return new AsyncLogger(Logger.getLogger(name, bundleName), name);
        } catch (ClassCastException e) {
            throw new IllegalStateException("The LogManager was not properly installed (you must set the \"java.util.logging.manager\" system property to \"" + LogManager.class.getName() + "\")");
        }
    }

    /**
     * Construct a new instance of an actual logger.
     *
     * @param delegate the delegate logger
     * @param name     the fully-qualified name of this node
     */
    AsyncLogger(final Logger delegate, final String name) {
        // Don't set up the bundle in the parent...
        super(name, null);
        // We maintain our own level
        super.setLevel(Level.ALL);
        this.delegate = delegate;
    }

    // Serialization

    // TODO (jrp) fix this
    protected final Object writeReplace() throws ObjectStreamException {
        return new SerializedLogger(getName());
    }

    // Filter mgmt

    @Override
    public void setFilter(Filter filter) throws SecurityException {
        LogContext.checkAccess(delegate.getLogContext());
        delegate.setFilter(filter);
    }

    @Override
    public Filter getFilter() {
        return delegate.getFilter();
    }

    // Level mgmt

    /**
     * {@inheritDoc}  This implementation grabs a lock, so that only one thread may update the log level of any
     * logger at a time, in order to allow readers to never block (though there is a window where retrieving the
     * log level reflects an older effective level than the actual level).
     */
    public void setLevel(Level newLevel) throws SecurityException {
        LogContext.checkAccess(delegate.getLogContext());
        delegate.setLevel(newLevel);
    }

    /**
     * Set the log level by name.  Uses the parent logging context's name registry; otherwise behaves
     * identically to {@link #setLevel(java.util.logging.Level)}.
     *
     * @param newLevelName the name of the level to set
     *
     * @throws SecurityException if a security manager exists and if the caller does not have
     *                           LoggingPermission("control")
     */
    public void setLevelName(String newLevelName) throws SecurityException {
        delegate.setLevelName(newLevelName);
    }

    /**
     * Get the effective numerical log level, inherited from the parent.
     *
     * @return the effective level
     */
    public int getEffectiveLevel() {
        return delegate.getEffectiveLevel();
    }

    @Override
    public Level getLevel() {
        return delegate.getLevel();
    }

    @Override
    public boolean isLoggable(Level level) {
        return delegate.isLoggable(level);
    }

    // Attachment mgmt

    /**
     * Get the attachment value for a given key, or {@code null} if there is no such attachment.
     *
     * @param key the key
     * @param <V> the attachment value type
     *
     * @return the attachment, or {@code null} if there is none for this key
     */
    @SuppressWarnings({"unchecked"})
    public <V> V getAttachment(AttachmentKey<V> key) {
        return delegate.getAttachment(key);
    }

    /**
     * Attach an object to this logger under a given key.
     * A strong reference is maintained to the key and value for as long as this logger exists.
     *
     * @param key   the attachment key
     * @param value the attachment value
     * @param <V>   the attachment value type
     *
     * @return the old attachment, if there was one
     *
     * @throws SecurityException if a security manager exists and if the caller does not have {@code
     *                           LoggingPermission(control)}
     */
    public <V> V attach(AttachmentKey<V> key, V value) throws SecurityException {
        LogContext.checkSecurityAccess();
        return delegate.attach(key, value);
    }

    /**
     * Attach an object to this logger under a given key, if such an attachment does not already exist.
     * A strong reference is maintained to the key and value for as long as this logger exists.
     *
     * @param key   the attachment key
     * @param value the attachment value
     * @param <V>   the attachment value type
     *
     * @return the current attachment, if there is one, or {@code null} if the value was successfully attached
     *
     * @throws SecurityException if a security manager exists and if the caller does not have {@code
     *                           LoggingPermission(control)}
     */
    @SuppressWarnings({"unchecked"})
    public <V> V attachIfAbsent(AttachmentKey<V> key, V value) throws SecurityException {
        LogContext.checkSecurityAccess();
        return delegate.attachIfAbsent(key, value);
    }

    /**
     * Remove an attachment.
     *
     * @param key the attachment key
     * @param <V> the attachment value type
     *
     * @return the old value, or {@code null} if there was none
     *
     * @throws SecurityException if a security manager exists and if the caller does not have {@code
     *                           LoggingPermission(control)}
     */
    @SuppressWarnings({"unchecked"})
    public <V> V detach(AttachmentKey<V> key) throws SecurityException {
        LogContext.checkAccess(delegate.getLogContext());
        return delegate.detach(key);
    }

    // Handler mgmt

    @Override
    public void addHandler(Handler handler) throws SecurityException {
        LogContext.checkAccess(delegate.getLogContext());
        delegate.addHandler(handler);
    }

    @Override
    public void removeHandler(Handler handler) throws SecurityException {
        LogContext.checkAccess(delegate.getLogContext());
        delegate.removeHandler(handler);
    }

    @Override
    public Handler[] getHandlers() {
        return delegate.getHandlers();
    }

    /**
     * A convenience method to atomically replace the handler list for this logger.
     *
     * @param handlers the new handlers
     *
     * @throws SecurityException if a security manager exists and if the caller does not have {@code
     *                           LoggingPermission(control)}
     */
    public void setHandlers(final Handler[] handlers) throws SecurityException {
        LogContext.checkAccess(delegate.getLogContext());
        delegate.setHandlers(handlers);
    }

    /**
     * Atomically get and set the handler list for this logger.
     *
     * @param handlers the new handler set
     *
     * @return the old handler set
     *
     * @throws SecurityException if a security manager exists and if the caller does not have {@code
     *                           LoggingPermission(control)}
     */
    public Handler[] getAndSetHandlers(final Handler[] handlers) throws SecurityException {
        LogContext.checkAccess(delegate.getLogContext());
        return delegate.getAndSetHandlers(handlers);
    }

    /**
     * Atomically compare and set the handler list for this logger.
     *
     * @param expected    the expected list of handlers
     * @param newHandlers the replacement list of handlers
     *
     * @return {@code true} if the handler list was updated or {@code false} if the current handlers did not match the
     * expected handlers list
     *
     * @throws SecurityException if a security manager exists and if the caller does not have {@code
     *                           LoggingPermission(control)}
     */
    public boolean compareAndSetHandlers(final Handler[] expected, final Handler[] newHandlers) throws SecurityException {
        LogContext.checkAccess(delegate.getLogContext());
        return delegate.compareAndSetHandlers(expected, newHandlers);
    }

    /**
     * A convenience method to atomically get and clear all handlers.
     *
     * @throws SecurityException if a security manager exists and if the caller does not have {@code
     *                           LoggingPermission(control)}
     */
    public Handler[] clearHandlers() throws SecurityException {
        LogContext.checkAccess(delegate.getLogContext());
        return delegate.clearHandlers();
    }

    @Override
    public void setUseParentHandlers(boolean useParentHandlers) {
        delegate.setUseParentHandlers(useParentHandlers);
    }

    @Override
    public boolean getUseParentHandlers() {
        return delegate.getUseParentHandlers();
    }

    // Parent/child

    @Override
    public AsyncLogger getParent() {
        AsyncLogger parent = getAttachment(PARENT_KEY);
        if (parent == null) {
            final Logger parentLogger = delegate.getParent();
            if (parentLogger == null) {
                return null;
            }
            parent = new AsyncLogger(parentLogger, parentLogger.getName());
            final AsyncLogger appearing = attachIfAbsent(PARENT_KEY, parent);
            if (appearing != null) {
                parent = appearing;
            }
        }
        return parent;
    }

    /**
     * <b>Not allowed.</b>  This method may never be called.
     *
     * @throws SecurityException always
     */
    public void setParent(java.util.logging.Logger parent) {
        throw new SecurityException("setParent() disallowed");
    }

    /**
     * Get the log context to which this logger belongs.
     *
     * @return the log context
     */
    public LogContext getLogContext() {
        return delegate.getLogContext();
    }

    // Logger

    static final int OFF_INT = Level.OFF.intValue();

    static final int SEVERE_INT = Level.SEVERE.intValue();
    static final int WARNING_INT = Level.WARNING.intValue();
    static final int INFO_INT = Level.INFO.intValue();
    static final int CONFIG_INT = Level.CONFIG.intValue();
    static final int FINE_INT = Level.FINE.intValue();
    static final int FINER_INT = Level.FINER.intValue();
    static final int FINEST_INT = Level.FINEST.intValue();

    @Override
    public void log(final LogRecord record) {
        final int effectiveLevel = getEffectiveLevel();
        if (record.getLevel().intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logRaw(record);
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        if (FINER_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.FINER)
                        .setMessage("ENTRY")
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
        );
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        if (FINER_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.FINER)
                        .setMessage("ENTRY {0}")
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
                        .setParameters(param1)
        );
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        if (FINER_INT < getEffectiveLevel()) {
            return;
        }
        final StringBuilder builder = new StringBuilder("ENTRY");
        if (params != null) for (int i = 0; i < params.length; i++) {
            builder.append(" {").append(i).append('}');
        }
        logAsync(new Runner()
                        .setLevel(Level.FINER)
                        .setMessage(builder.toString())
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
                        .setParameters(params)
        );
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        if (FINER_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.FINER)
                        .setMessage("RETURN")
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
        );
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        if (FINER_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.FINER)
                        .setMessage("RETURN {0}")
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
                        .setParameters(result)
        );
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        if (FINER_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.FINER)
                        .setMessage("THROW")
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
                        .setThrown(thrown)
        );
    }

    @Override
    public void severe(final String msg) {
        if (SEVERE_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.SEVERE)
                        .setMessage(msg)
        );
    }

    @Override
    public void warning(final String msg) {
        if (WARNING_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.WARNING)
                        .setMessage(msg)
        );
    }

    @Override
    public void info(final String msg) {
        if (INFO_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.INFO)
                        .setMessage(msg)
        );
    }

    @Override
    public void config(final String msg) {
        if (CONFIG_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.CONFIG)
                        .setMessage(msg)
        );
    }

    @Override
    public void fine(final String msg) {
        if (FINE_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.FINE)
                        .setMessage(msg)
        );
    }

    @Override
    public void finer(final String msg) {
        if (FINER_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.FINER)
                        .setMessage(msg)
        );
    }

    @Override
    public void finest(final String msg) {
        if (FINEST_INT < getEffectiveLevel()) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(Level.FINEST)
                        .setMessage(msg)
        );
    }

    @Override
    public void log(final Level level, final String msg) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(level)
                        .setMessage(msg)
        );
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(level)
                        .setMessage(msg)
                        .setParameters(param1)
        );
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(level)
                        .setMessage(msg)
                        .setParameters(params)
        );
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(level)
                        .setMessage(msg)
                        .setThrown(thrown)
        );
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(level)
                        .setMessage(msg)
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
        );
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Object param1) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(level)
                        .setMessage(msg)
                        .setParameters(param1)
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
        );
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Object[] params) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(level)
                        .setMessage(msg)
                        .setParameters(params)
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
        );
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg, final Throwable thrown) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLevel(level)
                        .setMessage(msg)
                        .setSourceClassName(sourceClass)
                        .setSourceMethodName(sourceMethod)
                        .setThrown(thrown)
        );
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        // TODO (jrp) will MDC be an issue?
        service.submit(new Runnable() {
            @Override
            public void run() {
                delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg);
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Object param1) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        // TODO (jrp) will MDC be an issue?
        service.submit(new Runnable() {
            @Override
            public void run() {
                delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1);
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Object[] params) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        // TODO (jrp) will MDC be an issue?
        service.submit(new Runnable() {
            @Override
            public void run() {
                delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg, params);
            }
        });
    }

    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod, final String bundleName, final String msg, final Throwable thrown) {
        final int effectiveLevel = getEffectiveLevel();
        if (level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        // TODO (jrp) will MDC be an issue?
        service.submit(new Runnable() {
            @Override
            public void run() {
                delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown);
            }
        });
    }

    // alternate SPI hooks

    /**
     * SPI interface method to log a message at a given level, with a specific resource bundle.
     *
     * @param fqcn       the fully qualified class name of the first logger class
     * @param level      the level to log at
     * @param message    the message
     * @param bundleName the resource bundle name
     * @param style      the message format style
     * @param params     the log parameters
     * @param t          the throwable, if any
     */
    public void log(final String fqcn, final Level level, final String message, final String bundleName, final ExtLogRecord.FormatStyle style, final Object[] params, final Throwable t) {
        final int effectiveLevel = getEffectiveLevel();
        if (level == null || fqcn == null || message == null || level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLoggerClassName(fqcn)
                        .setLevel(level)
                        .setBundleName(bundleName)
                        .setFormatStyle(style)
                        .setMessage(message)
                        .setParameters(params)
                        .setThrown(t)
        );
    }

    /**
     * SPI interface method to log a message at a given level.
     *
     * @param fqcn    the fully qualified class name of the first logger class
     * @param level   the level to log at
     * @param message the message
     * @param style   the message format style
     * @param params  the log parameters
     * @param t       the throwable, if any
     */
    public void log(final String fqcn, final Level level, final String message, final ExtLogRecord.FormatStyle style, final Object[] params, final Throwable t) {
        final int effectiveLevel = getEffectiveLevel();
        if (level == null || fqcn == null || message == null || level.intValue() < effectiveLevel || effectiveLevel == OFF_INT) {
            return;
        }
        logAsync(new Runner()
                        .setLoggerClassName(fqcn)
                        .setLevel(level)
                        .setFormatStyle(style)
                        .setMessage(message)
                        .setParameters(params)
                        .setThrown(t)
        );
    }

    /**
     * SPI interface method to log a message at a given level.
     *
     * @param fqcn    the fully qualified class name of the first logger class
     * @param level   the level to log at
     * @param message the message
     * @param t       the throwable, if any
     */
    public void log(final String fqcn, final Level level, final String message, final Throwable t) {
        log(fqcn, level, message, ExtLogRecord.FormatStyle.MESSAGE_FORMAT, null, t);
    }

    public String toString() {
        return "Logger '" + getName() + "' in context " + delegate.getLogContext();
    }

    // TODO (jrp) async methods below

    private void logRaw(final LogRecord record) {
        logRaw(ExtLogRecord.wrap(record));
    }

    private void logRaw(final ExtLogRecord record) {
        // TODO (jrp) this might not be right
        // MDC needs to be copied
        // final Map<?, ?> mdc = MDC.fastCopyObject();
        record.copyAll(); // TODO (jrp) this is likely not all that fast
        // Launch a new thread
        service.submit(new Runnable() {
            @Override
            public void run() {
                final ExtLogRecord rec = new ExtLogRecord(record);
                // record.setMdc(mdc);
                delegate.logRaw(rec);
            }
        });
    }

    // TODO (jrp) this should probably just be removed
    private void logAsync(final Runner runner) {
        runner.submit();
    }

    private class Runner {
        private String loggerClassName;
        private Level level;
        private String message;
        private String bundleName;
        private FormatStyle formatStyle;
        private Object[] params;
        private Throwable thrown;
        private String sourceClassName;
        private String sourceMethodName;

        public Runner() {
            loggerClassName = LOGGER_CLASS_NAME;
            level = Level.ALL;
            formatStyle = FormatStyle.MESSAGE_FORMAT;
        }

        public Runner setParameters(final Object param) {
            params = new Object[] {param};
            return this;
        }

        public Runner setParameters(final Object... params) {
            if (params == null) {
                this.params = null;
            } else {
                this.params = Arrays.copyOf(params, params.length);
            }
            return this;
        }

        public Runner setLoggerClassName(final String loggerClassName) {
            this.loggerClassName = loggerClassName;
            return this;
        }

        public Runner setLevel(final Level level) {
            this.level = level;
            return this;
        }

        public Runner setMessage(final String message) {
            this.message = message;
            return this;
        }

        public Runner setBundleName(final String bundleName) {
            this.bundleName = bundleName;
            return this;
        }

        public Runner setFormatStyle(final FormatStyle formatStyle) {
            this.formatStyle = formatStyle;
            return this;
        }

        public Runner setThrown(final Throwable thrown) {
            this.thrown = thrown;
            return this;
        }

        public Runner setSourceClassName(final String sourceClassName) {
            this.sourceClassName = sourceClassName;
            return this;
        }

        public Runner setSourceMethodName(final String sourceMethodName) {
            this.sourceMethodName = sourceMethodName;
            return this;
        }

        public void submit() {
            // final Map<?, ?> mdc = MDC.copy();
            // TODO (jrp) the caller may need to be calculated
            service.submit(new Runnable() {
                @Override
                public void run() {
                    final ExtLogRecord record = new ExtLogRecord(level, message, formatStyle, loggerClassName);
                    record.setResourceBundleName(bundleName);
                    if (params != null) {
                        record.setParameters(params);
                    }
                    // record.setMdc(mdc);
                    // record.setMillis();
                    // record.setNdc();
                    // record.setSequenceNumber();
                    if (sourceClassName != null) {
                        record.setSourceClassName(sourceClassName);
                    }
                    // record.setSourceFileName();
                    // record.setSourceLineNumber();
                    if (sourceMethodName != null) {
                        record.setSourceMethodName(sourceMethodName);
                    }
                    // record.setThreadID();
                    // record.setThreadName();
                    if (thrown != null) {
                        record.setThrown(thrown);
                    }
                    delegate.logRaw(record);
                }
            });
        }
    }

}
