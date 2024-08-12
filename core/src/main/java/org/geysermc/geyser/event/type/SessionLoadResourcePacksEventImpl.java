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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SessionLoadResourcePacksEventImpl extends SessionLoadResourcePacksEvent {

    private final Map<String, ResourcePack> packs;
    private final Map<String, Collection<ResourcePackOption>> options;

    public SessionLoadResourcePacksEventImpl(GeyserSession session) {
        super(session);
        this.packs = new Object2ObjectLinkedOpenHashMap<>(Registries.RESOURCE_PACKS.get());
        this.options = new HashMap<>();
    }

    public @NonNull Map<String, ResourcePack> getPacks() {
        return packs;
    }

    public LinkedList<ResourcePackStackPacket.Entry> orderedPacks() {
        // TODO sort by priority here

        return new LinkedList<>();
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
        for (ResourcePackOption option : resourcePackOptions) {
            option.validate(resourcePack);
        }

        String packID = resourcePack.manifest().header().uuid().toString();
        if (packs.containsValue(resourcePack) || packs.containsKey(packID)) {
            return false;
        }

        String uuid = resourcePack.manifest().header().uuid().toString();
        packs.put(uuid, resourcePack);
        options.put(uuid, List.of(resourcePackOptions));
        return true;
    }

    @Override
    public Collection<ResourcePackOption> options(UUID resourcePack) {
        Collection<ResourcePackOption> packOptions = options.get(resourcePack.toString());
        return packOptions == null ? List.of() : Collections.unmodifiableCollection(packOptions);
    }

    @Override
    public boolean unregister(@NonNull UUID uuid) {
        options.remove(uuid.toString());
        return packs.remove(uuid.toString()) != null;
    }
}
