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

package org.geysermc.geyser.translator.level.block.entity;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

import java.util.List;
import java.util.UUID;

@BlockEntity(type = BlockEntityType.VAULT)
public class VaultBlockEntityTranslator extends BlockEntityTranslator {
    // Bedrock 1.21 does not send the position nor ID in the tag.
    @Override
    public NbtMap getBlockEntityTag(GeyserSession session, BlockEntityType type, int x, int y, int z, @Nullable NbtMap javaNbt, BlockState blockState) {
        NbtMapBuilder builder = NbtMap.builder();
        if (javaNbt != null) {
            translateTag(session, builder, javaNbt, blockState);
        }
        return builder.build();
    }

    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        NbtMap sharedData = javaNbt.getCompound("shared_data");

        NbtMap item = sharedData.getCompound("display_item");
        ItemMapping mapping = session.getItemMappings().getMapping(item.getString("id"));
        if (mapping == null) {
            bedrockNbt.putCompound("display_item", NbtMap.builder()
                    .putByte("Count", (byte) 0)
                    .putShort("Damage", (short) 0)
                    .putString("Name", "")
                    .putByte("WasPickedUp", (byte) 0).build());
        } else {
            int count = item.getInt("count");
            NbtMapBuilder bedrockItem = BedrockItemBuilder.createItemNbt(mapping, count, mapping.getBedrockData());
            // TODO handle components...
            bedrockNbt.putCompound("display_item", bedrockItem.build());
        }

        List<int[]> connectedPlayers = sharedData.getList("connected_players", NbtType.INT_ARRAY);
        LongList bedrockPlayers = new LongArrayList(connectedPlayers.size());
        for (int[] player : connectedPlayers) {
            UUID uuid = uuidFromIntArray(player);
            if (uuid.equals(session.getPlayerEntity().getUuid())) {
                bedrockPlayers.add(session.getPlayerEntity().getGeyserId());
            } else {
                PlayerEntity playerEntity = session.getEntityCache().getPlayerEntity(uuid);
                if (playerEntity != null) {
                    bedrockPlayers.add(playerEntity.getGeyserId());
                }
            }
        }
        bedrockNbt.putList("connected_players", NbtType.LONG, bedrockPlayers);

        // Fill this in, since as of Java 1.21, Bedrock always seems to include it, but Java assumes the default
        // if it is not sent over the network
        bedrockNbt.putFloat("connected_particle_range", (float) sharedData.getDouble("connected_particles_range", 4.5d));
    }

    // From ViaVersion! thank u!!
    private static UUID uuidFromIntArray(int[] parts) {
        return new UUID((long) parts[0] << 32 | (parts[1] & 0xFFFFFFFFL), (long) parts[2] << 32 | (parts[3] & 0xFFFFFFFFL));
    }
}
