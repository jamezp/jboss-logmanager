/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import java.lang.management.PlatformLoggingMXBean;
import java.util.Collections;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.LoggingPermission;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) we should implement this as a ServiceLoader service.
// TODO (jrp) note this should really go into 2.1.x for Java 8 as well
public class JBossPlatformLoggingMXBean implements PlatformLoggingMXBean {
    private static final LoggingPermission CONTROL_PERMISSION = new LoggingPermission("control", null);

    // Lazily load the name in the getter
    private volatile ObjectName objectName;

    @Override
    public List<String> getLoggerNames() {
        return Collections.list(getLogContext().getLoggerNames());
    }

    @Override
    public String getLoggerLevel(final String loggerName) {
        final LogContext logContext = getLogContext();
        final Logger logger = logContext.getLoggerIfExists(loggerName);
        final java.util.logging.Level level = logger == null ? null : logger.getLevel();
        return level == null ? "" : level.getName();
    }

    @Override
    public void setLoggerLevel(final String loggerName, final String levelName) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(CONTROL_PERMISSION);
        }
        getLogContext().getLogger(levelName).setLevel(levelName == null ? null : Level.parse(levelName));
    }

    @Override
    public String getParentLoggerName(final String loggerName) {
        final Logger logger = getLogContext().getLoggerIfExists(loggerName);
        if (logger != null) {
            final Logger parent = logger.getParent();
            return parent == null ? "" : parent.getName();
        }
        return null;
    }

    @Override
    public ObjectName getObjectName() {
        ObjectName result = objectName;
        if (result == null) {
            synchronized (this) {
                result = objectName;
                if (result == null) {
                    try {
                        result = objectName = ObjectName.getInstance(LogManager.LOGGING_MXBEAN_NAME);
                    } catch (MalformedObjectNameException e) {
                        // This is the same exception type the JDK itself throws
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        }
        return result;
    }

    private LogContext getLogContext() {
        return LogContext.getLogContext();
    }
}
