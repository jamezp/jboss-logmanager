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

import org.jboss.modules.Module;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class Modules {

    private static final boolean JBOSS_MODULES;

    static {
        boolean jbossModules = false;
        try {
            //noinspection ResultOfMethodCallIgnored
            Module.getStartTime();
            jbossModules = true;
        } catch (Throwable ignored) {
        }
        JBOSS_MODULES = jbossModules;
    }

    static String getModuleName(final Class<?> type) {
        if (JBOSS_MODULES) {
            return calculateModule(type);
        }
        return calculateJdkModule(type);
    }

    private static String calculateJdkModule(final Class<?> clazz) {
        final java.lang.Module module = clazz.getModule();
        if (module != null) {
            return module.getName();
        }
        return null;
    }

    private static String calculateModule(final Class<?> clazz) {
        final Module module = Module.forClass(clazz);
        if (module != null) {
            return module.getName();
        }
        return calculateJdkModule(clazz);
    }
}
