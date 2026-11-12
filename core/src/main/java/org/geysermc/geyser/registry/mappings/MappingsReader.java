/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.mappings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@FunctionalInterface
public interface MappingsReader<K, V> {

    void read(Context<K, V> context);

    record Context<K, V>(JsonObject mappings, @Nullable UUID packUuid, BiConsumer<K, V> consumer,
                         Consumer<String> errorConsumer) implements BiConsumer<K, V> {

        public void forEachMapping(BiConsumer<String, JsonElement> consumer) {
            // Sorted so registration conflicts don't depend on the file's property order
            mappings.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
        }

        public void reportError(String error) {
            errorConsumer.accept(error);
        }

        @Override
        public void accept(K key, V value) {
            consumer.accept(key, value);
        }
    }
}
