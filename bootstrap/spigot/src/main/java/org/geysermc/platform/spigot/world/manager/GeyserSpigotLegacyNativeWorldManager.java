/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.spigot.world.manager;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.platform.spigot.GeyserSpigotPlugin;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.data.MappingData;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;

import java.util.List;

/**
 * Used when block IDs need to be translated to the latest version
 */
public class GeyserSpigotLegacyNativeWorldManager extends GeyserSpigotNativeWorldManager {

    private final Int2IntMap oldToNewBlockId;

    public GeyserSpigotLegacyNativeWorldManager(GeyserSpigotPlugin plugin, boolean use3dBiomes) {
        super(plugin, use3dBiomes);
        IntList allBlockStates = adapter.getAllBlockStates();
        oldToNewBlockId = new Int2IntOpenHashMap(allBlockStates.size());
        ProtocolVersion serverVersion = plugin.getServerProtocolVersion();
        List<Pair<Integer, Protocol>> protocolList = ProtocolRegistry.getProtocolPath(MinecraftConstants.PROTOCOL_VERSION,
                serverVersion.getVersion());
        for (int oldBlockId : allBlockStates) {
            int newBlockId = oldBlockId;
            // protocolList should *not* be null; we checked for that before initializing this class
            for (int i = protocolList.size() - 1; i >= 0; i--) {
                MappingData mappingData = protocolList.get(i).getValue().getMappingData();
                if (mappingData != null) {
                    newBlockId = mappingData.getNewBlockStateId(newBlockId);
                }
            }
            oldToNewBlockId.put(oldBlockId, newBlockId);
        }
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        int nativeBlockId = super.getBlockAt(session, x, y, z);
        return oldToNewBlockId.getOrDefault(nativeBlockId, nativeBlockId);
    }

    @Override
    public boolean isLegacy() {
        return true;
    }
}
