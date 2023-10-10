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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.ResourcePackCDNEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeyserDefineResourcePacksEventImpl extends GeyserDefineResourcePacksEvent {

    private final Map<String, ResourcePack> packs;
    private final Map<String, ResourcePackCDNEntry> cdnEntries;

    public GeyserDefineResourcePacksEventImpl(Map<String, ResourcePack> packMap, List<ResourcePackCDNEntry> cdnEntries) {
        this.packs = packMap;
        this.cdnEntries = new HashMap<>();
        cdnEntries.forEach(entry -> this.cdnEntries.put(entry.uuid().toString(), entry));
    }

    public @NonNull Map<String, ResourcePack> getPacks() {
        return packs;
    }

    @Override
    public @NonNull List<ResourcePack> resourcePacks() {
        return List.copyOf(packs.values());
    }

    @Override
    public @NonNull List<ResourcePackCDNEntry> cdnEntries() {
        return List.copyOf(cdnEntries.values());
    }

    @Override
    public boolean register(@NonNull ResourcePack resourcePack) {
        String packID = resourcePack.manifest().header().uuid().toString();
        if (packs.containsValue(resourcePack) || packs.containsKey(packID) || cdnEntries.containsKey(packID)) {
            return false;
        }
        packs.put(resourcePack.manifest().header().uuid().toString(), resourcePack);
        return true;
    }

    @Override
    public boolean register(@NonNull ResourcePackCDNEntry entry) {
        String packID = entry.uuid().toString();
        if (packs.containsKey(packID) || cdnEntries.containsValue(entry) || cdnEntries.containsKey(packID)) {
            return false;
        }
        cdnEntries.put(packID, entry);
        return true;
    }

    @Override
    public boolean unregister(@NonNull UUID uuid) {
        if (packs.containsKey(uuid.toString())) {
            return packs.remove(uuid.toString()) != null;
        } else if (cdnEntries.containsKey(uuid.toString())) {
            return cdnEntries.remove(uuid.toString()) != null;
        } else {
            return false;
        }
    }
}
