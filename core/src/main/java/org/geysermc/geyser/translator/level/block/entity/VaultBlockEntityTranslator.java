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
import org.geysermc.geyser.item.parser.ItemStackParser;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.EntityUtils;
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
        bedrockNbt.putCompound("display_item", ItemStackParser.javaItemStackToBedrock(session, sharedData.getCompound("display_item")).build());

        List<int[]> connectedPlayers = sharedData.getList("connected_players", NbtType.INT_ARRAY);
        LongList bedrockPlayers = new LongArrayList(connectedPlayers.size());
        for (int[] player : connectedPlayers) {
            UUID uuid = EntityUtils.uuidFromIntArray(player);
            if (uuid.equals(session.getPlayerEntity().uuid())) {
                bedrockPlayers.add(session.getPlayerEntity().geyserId());
            } else {
                PlayerEntity playerEntity = session.getEntityCache().getPlayerEntity(uuid);
                if (playerEntity != null) {
                    bedrockPlayers.add(playerEntity.geyserId());
                }
            }
        }
        bedrockNbt.putList("connected_players", NbtType.LONG, bedrockPlayers);

        // Fill this in, since as of Java 1.21, Bedrock always seems to include it, but Java assumes the default
        // if it is not sent over the network
        bedrockNbt.putFloat("connected_particle_range", (float) sharedData.getDouble("connected_particles_range", 4.5d));
    }
}
