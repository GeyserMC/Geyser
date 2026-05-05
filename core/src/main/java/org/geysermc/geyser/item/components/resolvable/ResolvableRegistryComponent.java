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

package org.geysermc.geyser.item.components.resolvable;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

public record ResolvableRegistryComponent<T>(DataComponentType<Holder<T>> type, JavaRegistryKey<?> registry, Key reference) implements ResolvableComponent<Holder<T>> {

    public static ResolvableRegistryComponent<?> parse(JsonObject object) {
        //noinspection unchecked
        DataComponentType<Holder<?>> component = (DataComponentType<Holder<?>>) DataComponentTypes.fromKey(MinecraftKey.key(object.get("component").getAsString()));
        JavaRegistryKey<?> registry = JavaRegistries.fromKey(MinecraftKey.key(object.get("registry").getAsString()));
        Key reference = MinecraftKey.key(object.get("reference").getAsString());
        //noinspection rawtypes
        return new ResolvableRegistryComponent(component, registry, reference);
    }

    @Override
    public @Nullable Holder<T> resolve(GeyserSession session) {
        int numericId = registry.networkId(session, reference);
        if (numericId == -1) {
            return null;
        }
        return Holder.ofId(numericId);
    }
}
