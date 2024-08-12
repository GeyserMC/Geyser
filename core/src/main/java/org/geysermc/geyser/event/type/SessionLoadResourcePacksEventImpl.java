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
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.ResourcePackManifest;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;
import org.geysermc.geyser.api.pack.option.SubpackOption;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class SessionLoadResourcePacksEventImpl extends SessionLoadResourcePacksEvent {

    @Getter
    private final Map<UUID, ResourcePack> packs;
    private final Map<UUID, Collection<ResourcePackOption>> options = new HashMap<>();

    public SessionLoadResourcePacksEventImpl(GeyserSession session) {
        super(session);
        this.packs = new Object2ObjectLinkedOpenHashMap<>(Registries.RESOURCE_PACKS.get());
        this.packs.values().forEach(
            pack -> options.put(pack.manifest().header().uuid(), pack.defaultOptions())
        );
    }

    public LinkedList<ResourcePackStackPacket.Entry> orderedPacks() {
        TreeSet<Map.Entry<ResourcePack, Integer>> sortedPacks = packs.values().stream()
            // Map each ResourcePack to a pair of (ResourcePack, Priority)
            .map(pack -> new AbstractMap.SimpleEntry<>(pack, getPriority(pack)))
            // Sort by priority in descending order
            .collect(Collectors.toCollection(() -> new TreeSet<>(Map.Entry.comparingByValue(Comparator.naturalOrder()))));

        // Convert the sorted entries to a LinkedList of ResourcePackStackPacket.Entry
        return sortedPacks.stream()
            .map(entry -> {
                ResourcePackManifest.Header header = entry.getKey().manifest().header();
                return new ResourcePackStackPacket.Entry(
                    header.uuid().toString(),
                    header.version().toString(),
                    getSubpackName(header.uuid())
                );
            })
            .collect(Collectors.toCollection(LinkedList::new));
    }

    // Helper method to get the priority of a ResourcePack
    private int getPriority(ResourcePack pack) {
        return options.get(pack.manifest().header().uuid()).stream()
            .filter(option -> option instanceof PriorityOption)
            .mapToInt(option -> ((PriorityOption) option).priority())
            .max()
            .orElse(PriorityOption.NORMAL.priority());
    }


    public List<ResourcePacksInfoPacket.Entry> infoPacketEntries() {
        List<ResourcePacksInfoPacket.Entry> entries = new ArrayList<>();

        for (ResourcePack pack : packs.values()) {
            ResourcePackManifest.Header header = pack.manifest().header();
            entries.add(new ResourcePacksInfoPacket.Entry(
                header.uuid().toString(), header.version().toString(), pack.codec().size(), pack.contentKey(),
                getSubpackName(header.uuid()), header.uuid().toString(), false, false)
            );
        }

        return entries;
    }

    private String getSubpackName(UUID uuid) {
        return options.get(uuid).stream()
            .filter(option -> option instanceof SubpackOption)
            .map(option -> ((SubpackOption) option).subpackName())
            .findFirst()
            .orElse("");  // Return an empty string if none is found
    }

    @Override
    public @NonNull List<ResourcePack> resourcePacks() {
        return List.copyOf(packs.values());
    }

    @Override
    public boolean register(@NonNull ResourcePack resourcePack) {
        return register(resourcePack, PriorityOption.NORMAL);
    }

    @Override
    public boolean register(@NonNull ResourcePack resourcePack, ResourcePackOption... resourcePackOptions) {

        // Validate options, check for duplicates
        Set<ResourcePackOption.Type> types = new HashSet<>();
        for (ResourcePackOption option : resourcePackOptions) {
            option.validate(resourcePack);
            if (!types.add(option.type())) {
                throw new IllegalArgumentException("Duplicate resource pack option " + option + "!");
            }
        }
        types.clear();

        UUID uuid = resourcePack.manifest().header().uuid();
        if (packs.containsValue(resourcePack) || packs.containsKey(uuid)) {
            return false;
        }

        packs.put(uuid, resourcePack);
        options.put(uuid, List.of(resourcePackOptions));
        return true;
    }

    @Override
    public Collection<ResourcePackOption> options(UUID uuid) {
        return Collections.unmodifiableCollection(options.get(uuid));
    }

    @Override
    public boolean unregister(@NonNull UUID uuid) {
        options.remove(uuid);
        return packs.remove(uuid) != null;
    }
}
