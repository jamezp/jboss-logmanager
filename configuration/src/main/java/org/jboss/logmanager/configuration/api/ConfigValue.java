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

package org.jboss.logmanager.configuration.api;

import java.util.Set;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) rename ConfigValue or just Value maybe, possibly move the package too
// TODO (jrp) need a way to store the expression value too, if applicable
public interface ConfigValue<T> extends Comparable<ConfigValue<? extends T>> {

    String name();

    default String moduleName() {
        return null;
    }

    String className();

    Set<String> constructorPropertyNames();

    String constructorProperty(String name);

    Set<String> postConstructMethods();

    Set<String> propertyNames();

    // TODO (jrp) should this just be a string?
    String property(String name);

    T value();

    @Override
    default int compareTo(ConfigValue<? extends T> o) {
        return name().compareTo(o.name());
    }
}
