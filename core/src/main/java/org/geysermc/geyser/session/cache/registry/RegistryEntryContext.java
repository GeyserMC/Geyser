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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Optional;

import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;

/**
 * Used to store context around a single registry entry when reading said entry's NBT.
 *
 * @param entry the registry entry being read.
 * @param keyIdMap a map for each of the resource location's in the registry and their respective network IDs.
 * @param session the Geyser session. Only empty during testing.
 */
public record RegistryEntryContext(RegistryEntry entry, Object2IntMap<Key> keyIdMap, Optional<GeyserSession> session) {

    // TODO: not a fan of this. With JavaRegistryKey#key now being a thing, I'd rather have that always used, so that registry readers won't have to worry
    // about using the right method. This would require pre-populating all data-driven registries with default (probably null) values before actually decoding the data from the registy packet.
    // This could also be helpful in the feature when a data-driven registry reader needs to use an element from another data-driven registry
    public int getNetworkId(Key registryKey) {
        return keyIdMap.getOrDefault(registryKey, -1);
    }

    public Key id() {
        return entry.getId();
    }

    // Not annotated as nullable because data should never be null here
    public NbtMap data() {
        return entry.getData();
    }

    public String deserializeDescription() {
        return session.map(present -> MessageTranslator.deserializeDescription(present, data())).orElse("MISSING GEYSER SESSION");
    }
}
