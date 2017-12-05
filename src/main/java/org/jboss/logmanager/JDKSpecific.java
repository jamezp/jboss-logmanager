package org.jboss.logmanager;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import org.jboss.modules.Module;
import org.jboss.modules.Version;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class JDKSpecific {
    private JDKSpecific() {}

    private static final Gateway GATEWAY;
    private static final boolean JBOSS_MODULES;

    static {
        GATEWAY = AccessController.doPrivileged(new PrivilegedAction<Gateway>() {
            public Gateway run() {
                return new Gateway();
            }
        });
        boolean jbossModules = false;
        try {
            Module.getStartTime();
            jbossModules = true;
        } catch (Throwable ignored) {}
        JBOSS_MODULES = jbossModules;
    }

    static final class Gateway extends SecurityManager {
        protected Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }

    static Class<?> findCallingClass(Set<ClassLoader> rejectClassLoaders) {
        for (Class<?> caller : GATEWAY.getClassContext()) {
            final ClassLoader classLoader = caller.getClassLoader();
            if (classLoader != null && ! rejectClassLoaders.contains(classLoader)) {
                return caller;
            }
        }
        return null;
    }

    static void calculateCaller(ExtLogRecord logRecord) {
        final String loggerClassName = logRecord.getLoggerClassName();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final Class<?>[] classes = GATEWAY.getClassContext();
        // The stack trace may be missing classes, but the class context is not, so if we find a mismatch, we skip the class context items.
        int i = 1, j = 0;
        Class<?> clazz = classes[i++];
        StackTraceElement element = stackTrace[j++];
        boolean found = false;
        for (;;) {
            if (clazz.getName().equals(element.getClassName())) {
                if (clazz.getName().equals(loggerClassName)) {
                    // next entry could be the one we want!
                    found = true;
                } else {
                    if (found) {
                        logRecord.setSourceClassName(element.getClassName());
                        logRecord.setSourceMethodName(element.getMethodName());
                        logRecord.setSourceFileName(element.getFileName());
                        logRecord.setSourceLineNumber(element.getLineNumber());
                        if (JBOSS_MODULES) {
                            calculateModule(logRecord, clazz);
                        }
                        return;
                    }
                }
                if (j == classes.length) {
                    logRecord.setUnknownCaller();
                    return;
                }
                element = stackTrace[j ++];
            }
            if (i == classes.length) {
                logRecord.setUnknownCaller();
                return;
            }
            clazz = classes[i ++];
        }
    }

    private static void calculateModule(final ExtLogRecord logRecord, final Class<?> clazz) {
        final Module module = Module.forClass(clazz);
        if (module != null) {
            logRecord.setSourceModuleName(module.getName());
            final Version version = module.getVersion();
            if (version != null) {
                logRecord.setSourceModuleVersion(version.toString());
            } else {
                logRecord.setSourceModuleVersion(null);
            }
        }
    }
}
