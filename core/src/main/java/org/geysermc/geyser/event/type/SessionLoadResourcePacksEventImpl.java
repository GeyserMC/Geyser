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

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.ResourcePackManifest;
import org.geysermc.geyser.api.pack.UrlPackCodec;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;
import org.geysermc.geyser.pack.GeyserResourcePack;
import org.geysermc.geyser.pack.ResourcePackHolder;
import org.geysermc.geyser.pack.option.OptionHolder;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class SessionLoadResourcePacksEventImpl extends SessionLoadResourcePacksEvent {

    @Getter
    private final Map<UUID, ResourcePackHolder> packs;
    private final Map<UUID, OptionHolder> options;

    public SessionLoadResourcePacksEventImpl(GeyserSession session) {
        super(session);
        this.packs = new Object2ObjectLinkedOpenHashMap<>(Registries.RESOURCE_PACKS.get());
        this.options = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public @NonNull List<ResourcePack> resourcePacks() {
        return packs.values().stream().map(ResourcePackHolder::pack).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean register(@NonNull ResourcePack resourcePack) {
        return register(resourcePack, PriorityOption.NORMAL);
    }

    @Override
    public boolean register(@NonNull ResourcePack resourcePack, @Nullable ResourcePackOption<?>... options) {
        GeyserResourcePack pack = validate(resourcePack);

        UUID uuid = resourcePack.uuid();
        if (packs.containsKey(uuid)) {
            return false;
        }

        packs.put(uuid, ResourcePackHolder.of(pack));

        // register options
        registerOption(resourcePack, options);
        return true;
    }

    @Override
    public void registerOptions(@NonNull UUID uuid, @NonNull ResourcePackOption<?>... options) {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(options);
        ResourcePackHolder holder = packs.get(uuid);
        if (holder == null) {
            throw new IllegalArgumentException("Pack with uuid %s not registered yet!".formatted(uuid));
        }

        registerOption(holder.pack(), options);
    }

    @Override
    public Collection<ResourcePackOption<?>> options(@NonNull UUID uuid) {
        Objects.requireNonNull(uuid);
        ResourcePackHolder packHolder = packs.get(uuid);
        if (packHolder == null) {
            throw new IllegalArgumentException("ResourcePack with " + uuid + " not found, unable to provide options");
        }

        OptionHolder optionHolder = options.getOrDefault(uuid, new OptionHolder());
        return optionHolder.immutableValues(packHolder.optionHolder());
    }

    @Override
    public boolean unregister(@NonNull UUID uuid) {
        options.remove(uuid);
        return packs.remove(uuid) != null;
    }

    private void registerOption(@NonNull ResourcePack resourcePack, @Nullable ResourcePackOption<?>... options) {
        if (options == null) {
            return;
        }

        GeyserResourcePack pack = validate(resourcePack);

        OptionHolder holder = this.options.computeIfAbsent(pack.uuid(), $ -> new OptionHolder());
        holder.add(options);
        holder.validateOptions(pack);
    }

    // Methods used internally for e.g. ordered packs, or resource pack entries

    public List<ResourcePackStackPacket.Entry> orderedPacks() {
        TreeSet<Map.Entry<GeyserResourcePack, Double>> sortedPacks = packs.values().stream()
            // Map each ResourcePack to a pair of (GeyserResourcePack, Priority)
            .map(holder -> new AbstractMap.SimpleEntry<>(holder.pack(), priority(holder.pack())))
            // Sort by priority in ascending order
            .collect(Collectors.toCollection(() -> new TreeSet<>(Map.Entry.comparingByValue(Comparator.naturalOrder()))));

        return sortedPacks.stream()
            .map(entry -> {
                ResourcePackManifest.Header header = entry.getKey().manifest().header();
                return new ResourcePackStackPacket.Entry(
                    header.uuid().toString(),
                    header.version().toString(),
                    subpackName(entry.getKey())
                );
            })
            .toList();
    }

    public List<ResourcePacksInfoPacket.Entry> infoPacketEntries() {
        List<ResourcePacksInfoPacket.Entry> entries = new ArrayList<>();

        for (ResourcePackHolder holder : packs.values()) {
            GeyserResourcePack pack = holder.pack();
            ResourcePackManifest.Header header = pack.manifest().header();
            entries.add(new ResourcePacksInfoPacket.Entry(
                header.uuid().toString(), header.version().toString(), pack.codec().size(), pack.contentKey(),
                subpackName(pack), header.uuid().toString(), false, false, false)
            );
        }

        return entries;
    }

    public List<ResourcePacksInfoPacket.CDNEntry> cdnEntries() {
        List<ResourcePacksInfoPacket.CDNEntry> entries = new ArrayList<>();

        for (ResourcePackHolder holder : this.packs.values()) {
            GeyserResourcePack pack = holder.pack();
            ResourcePackManifest.Header header = pack.manifest().header();
            if (pack.codec() instanceof UrlPackCodec urlPackCodec) {
                entries.add(new ResourcePacksInfoPacket.CDNEntry(
                    header.uuid() + "_" + header.version(), urlPackCodec.url()));
            }
        }

        return entries;
    }

    // Helper methods to get the options for a ResourcePack

    public <T> T getValue(UUID uuid, ResourcePackOption.Type type, T defaultValue) {
        OptionHolder holder = options.get(uuid);
        OptionHolder defaultHolder = packs.get(uuid).optionHolder();
        Objects.requireNonNull(defaultHolder); // should never be null

        return OptionHolder.getWithFallbacks(type, holder, defaultHolder, defaultValue);
    }

    private double priority(GeyserResourcePack pack) {
        return getValue(pack.uuid(), ResourcePackOption.Type.PRIORITY, 5);
    }

    private String subpackName(GeyserResourcePack pack) {
        return getValue(pack.uuid(), ResourcePackOption.Type.SUBPACK, "");
    }

    // Helper method to validate a pack

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
