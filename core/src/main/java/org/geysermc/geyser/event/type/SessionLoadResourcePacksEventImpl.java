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
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.ResourcePackCDNEntry;
import org.geysermc.geyser.session.GeyserSession;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SessionLoadResourcePacksEventImpl extends SessionLoadResourcePacksEvent {

    private final Map<String, ResourcePack> packs;

    private final List<ResourcePackCDNEntry> cdnEntries;

    public SessionLoadResourcePacksEventImpl(GeyserSession session, Map<String, ResourcePack> packMap, List<ResourcePackCDNEntry> cdnEntries) {
        super(session);
        this.packs = packMap;
        this.cdnEntries = cdnEntries;
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
        return List.copyOf(cdnEntries);
    }

    @Override
    public boolean register(@NonNull ResourcePack resourcePack) {
        String packID = resourcePack.manifest().header().uuid().toString();
        if (packs.containsValue(resourcePack) || packs.containsKey(packID)
                || !cdnEntries.isEmpty() && cdnEntries.stream().anyMatch(entry -> entry.uuid().toString().equals(packID))) {
            return false;
        }
        packs.put(resourcePack.manifest().header().uuid().toString(), resourcePack);
        return true;
    }

    @Override
    public boolean register(@NonNull ResourcePackCDNEntry entry) {
        UUID packID = entry.uuid();
        if (packs.containsKey(packID.toString()) || cdnEntries.contains(entry)
                || !cdnEntries.isEmpty() && cdnEntries.stream().anyMatch(cdnEntry -> cdnEntry.uuid().equals(packID))) {
            return false;
        }
        cdnEntries.add(entry);
        return true;
    }

    @Override
    public boolean unregister(@NonNull UUID uuid) {
        if (packs.containsKey(uuid.toString())) {
            return packs.remove(uuid.toString()) != null;
        } else if (!cdnEntries.isEmpty() && cdnEntries.stream().anyMatch(entry -> entry.uuid().equals(uuid))) {
            return cdnEntries.removeIf(entry -> entry.uuid().equals(uuid));
        } else {
            return false;
        }
    }
}
