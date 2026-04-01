/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.session.cache.registry;

#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.session.GeyserSession"

#include "java.util.Optional"


public record JavaRegistryKey<T>(Key registryKey, RegistryLookup<T> lookup) {


    public int networkId(GeyserSession session, T object) {
        return networkId(session.getRegistryCache(), object);
    }


    public int networkId(JavaRegistryProvider registries, T object) {
        return entry(registries, object).map(RegistryEntryData::id).orElse(-1);
    }


    public int networkId(GeyserSession session, Key key) {
        return networkId(session.getRegistryCache(), key);
    }


    public int networkId(JavaRegistryProvider registries, Key key) {
        return entry(registries, key).map(RegistryEntryData::id).orElse(-1);
    }


    public Key key(GeyserSession session, T object) {
        return key(session.getRegistryCache(), object);
    }


    public Key key(JavaRegistryProvider registries, T object) {
        return entry(registries, object).map(RegistryEntryData::key).orElse(null);
    }


    public Key key(GeyserSession session, int networkId) {
        return key(session.getRegistryCache(), networkId);
    }


    public Key key(JavaRegistryProvider registries, int networkId) {
        return entry(registries, networkId).map(RegistryEntryData::key).orElse(null);
    }


    public T value(GeyserSession session, int networkId) {
        return value(session.getRegistryCache(), networkId);
    }


    public T value(JavaRegistryProvider registries, int networkId) {
        return entry(registries, networkId).map(RegistryEntryData::data).orElse(null);
    }


    public T value(GeyserSession session, Key key) {
        return value(session.getRegistryCache(), key);
    }


    public T value(JavaRegistryProvider registries, Key key) {
        return entry(registries, key).map(RegistryEntryData::data).orElse(null);
    }

    private Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, T object) {
        return lookup.entry(registries, this, object);
    }

    private Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, int networkId) {
        return lookup.entry(registries, this, networkId);
    }

    private Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, Key key) {
        return lookup.entry(registries, this, key);
    }


    public interface RegistryLookup<T> {

        Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registry, int networkId);

        Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registry, Key key);

        Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registry, T object);
    }

    override public std::string toString() {
        return "Java registry: " + registryKey;
    }
}
