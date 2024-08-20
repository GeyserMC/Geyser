/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.event.type;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;
import org.geysermc.geyser.pack.GeyserResourcePack;
import org.geysermc.geyser.pack.ResourcePackHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class GeyserDefineResourcePacksEventImpl extends GeyserDefineResourcePacksEvent {
    private final Map<UUID, ResourcePackHolder> packs;

    public GeyserDefineResourcePacksEventImpl(Map<UUID, ResourcePackHolder> packMap) {
        this.packs = packMap;
    }

    @Override
    public @NonNull List<ResourcePack> resourcePacks() {
        return packs.values().stream().map(ResourcePackHolder::pack).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean register(@NonNull ResourcePack resourcePack, @Nullable ResourcePackOption<?>... options) {
        GeyserResourcePack pack = validate(resourcePack);

        UUID uuid = resourcePack.uuid();
        if (packs.containsKey(uuid)) {
            return false;
        }

        ResourcePackHolder holder = ResourcePackHolder.of(pack);
        packs.put(uuid, holder);

        // register options
        registerOption(holder, options);
        return true;
    }

    @Override
    public void registerOptions(@NonNull UUID uuid, @NonNull ResourcePackOption<?>... options) {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(options);

        ResourcePackHolder packHolder = packs.get(uuid);
        if (packHolder == null) {
            throw new IllegalArgumentException("ResourcePack with " + uuid + " not found, unable to provide options");
        }

        registerOption(packHolder, options);
    }

    @Override
    public Collection<ResourcePackOption<?>> options(@NonNull UUID uuid) {
        Objects.requireNonNull(uuid);
        ResourcePackHolder packHolder = packs.get(uuid);
        if (packHolder == null) {
            throw new IllegalArgumentException("ResourcePack with " + uuid + " not found, unable to provide options");
        }

        return packHolder.optionHolder().immutableValues();
    }

    @Override
    public boolean unregister(@NonNull UUID uuid) {
        return packs.remove(uuid) != null;
    }

    private void registerOption(@NonNull ResourcePackHolder holder, @Nullable ResourcePackOption<?>... options) {
        if (options == null) {
            return;
        }

        holder.optionHolder().add(options);
        holder.optionHolder().validateOptions(holder.pack());
    }

    private GeyserResourcePack validate(@NonNull ResourcePack resourcePack) {
        Objects.requireNonNull(resourcePack);
        if (resourcePack instanceof GeyserResourcePack geyserResourcePack) {
            return geyserResourcePack;
        } else {
            throw new IllegalArgumentException("Unknown resource pack implementation: %s".
                formatted(resourcePack.getClass().getSuperclass().getName()));
        }
    }
}
