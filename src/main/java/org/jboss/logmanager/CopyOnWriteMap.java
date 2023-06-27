/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

final class CopyOnWriteMap<K, V> implements ConcurrentMap<K, V>, Cloneable {
    private volatile Map<K, V> delegate;
    private static final VarHandle DELEGATE_HANDLER;

    static {
        try {
            DELEGATE_HANDLER = MethodHandles.lookup().findVarHandle(CopyOnWriteMap.class, "delegate", Map.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    CopyOnWriteMap() {
        delegate = Map.of();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public V get(final Object key) {
        return delegate.get(key);
    }

    @Override
    public V put(final K key, final V value) {
        final Map<K, V> newMap = new LinkedHashMap<>(delegate);
        try {
            return newMap.put(key, value);
        } finally {
            DELEGATE_HANDLER.setVolatile(this, Map.copyOf(newMap));
        }
    }

    @Override
    public V remove(final Object key) {
        final Map<K, V> newMap = new LinkedHashMap<>(delegate);
        try {
            return newMap.remove(key);
        } finally {
            DELEGATE_HANDLER.setVolatile(this, Map.copyOf(newMap));
        }
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        final Map<K, V> newMap = new LinkedHashMap<>(delegate);
        newMap.putAll(m);
        DELEGATE_HANDLER.setVolatile(this, Map.copyOf(newMap));
    }

    @Override
    public void clear() {
        DELEGATE_HANDLER.setVolatile(this, Map.of());
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {
        delegate.forEach(action);
    }

    @Override
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
        delegate.replaceAll(function);
    }

    @Override
    public V putIfAbsent(final K key, final V value) {
        final Map<K, V> newMap = new LinkedHashMap<>(delegate);
        try {
            return newMap.putIfAbsent(key, value);
        } finally {
            DELEGATE_HANDLER.setVolatile(this, Map.copyOf(newMap));
        }
    }

    @Override
    public boolean remove(final Object key, final Object value) {
        return delegate.remove(key, value);
    }

    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(final K key, final V value) {
        return delegate.replace(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(final K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.compute(key, remappingFunction);
    }

    @Override
    public V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return delegate.merge(key, value, remappingFunction);
    }

    @SuppressWarnings("unchecked")
    public CopyOnWriteMap<K, V> clone() {
        try {
            return (CopyOnWriteMap<K, V>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException();
        }
    }
}
