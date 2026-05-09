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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record ResolvableHolderSetComponent(DataComponentType<HolderSet> type, Optional<JavaRegistryKey<?>> registry, List<Key> references) implements ResolvableComponent<HolderSet> {

    public static ResolvableHolderSetComponent parse(DataComponentType<HolderSet> type, JsonObject object) {
        Optional<JavaRegistryKey<?>> registry = Optional.ofNullable(object.get("registry"))
            .map(JsonElement::getAsString)
            .map(MinecraftKey::key)
            .map(JavaRegistries::fromKey);
        List<Key> references = Optional.ofNullable(object.get("references"))
            .map(JsonElement::getAsJsonArray)
            .stream()
            .flatMap(array -> array.asList().stream())
            .map(JsonElement::getAsString)
            .map(MinecraftKey::key)
            .toList();
        return new ResolvableHolderSetComponent(type, registry, references);
    }

    @Override
    public @Nullable HolderSet resolve(GeyserSession session) {
        return registry.map(theRegistry -> references.stream()
            .mapToInt(key -> theRegistry.networkId(session, key))
            .toArray())
            .map(HolderSet::new)
            .orElseGet(() -> new HolderSet(new int[0]));
    }
}
