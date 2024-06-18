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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.common.util.TriConsumer;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            NbtMap componentsTag = item.getCompound("components");
            NbtMapBuilder itemAsNbt;
            if (!componentsTag.isEmpty()) {
                DataComponents components = new DataComponents(new HashMap<>());
                for (Map.Entry<String, Object> entry : componentsTag.entrySet()) {
                    var consumer = DATA_COMPONENT_DECODERS.get(entry.getKey());
                    if (consumer != null) {
                        consumer.accept(session, (NbtMap) entry.getValue(), components);
                    }
                }
                ItemData bedrockItem = ItemTranslator.translateToBedrock(session, mapping.getJavaItem(), mapping, count, components).build();
                itemAsNbt = BedrockItemBuilder.createItemNbt(mapping, bedrockItem.getCount(), bedrockItem.getDamage());
                if (bedrockItem.getTag() != null) {
                    itemAsNbt.putCompound("tag", bedrockItem.getTag());
                }
            } else {
                itemAsNbt = BedrockItemBuilder.createItemNbt(mapping, count, mapping.getBedrockData());
            }
            bedrockNbt.putCompound("display_item", itemAsNbt.build());
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

    // This might be easier to maintain in the long run so items don't have two translate methods.
    // Also, it's not out of the question that block entities get the data component treatment, likely rendering this useless.
    // The goal is to just translate the basics so clients know what potion is roughly present, and that any enchantment even exists.
    private static final Map<String, TriConsumer<GeyserSession, NbtMap, DataComponents>> DATA_COMPONENT_DECODERS = Map.of(
            "minecraft:potion_contents", (session, tag, components) -> {
                String potionId = tag.getString("potion");
                Potion potion = Potion.getByJavaIdentifier(potionId);
                components.put(DataComponentType.POTION_CONTENTS, potion.toComponent());
            },
            "minecraft:enchantments", (session, tag, components) -> { // Enchanted books already have glint. Translating them doesn't matter.
                NbtMap levels = tag.getCompound("levels");
                List<Enchantment> enchantmentRegistry = session.getRegistryCache().enchantments().values();
                Int2ObjectMap<Integer> enchantments = new Int2ObjectOpenHashMap<>(levels.size());
                for (Map.Entry<String, Object> entry : levels.entrySet()) {
                    for (int i = 0; i < enchantmentRegistry.size(); i++) {
                        if (enchantmentRegistry.get(i).identifier().equals(entry.getKey())) {
                            enchantments.put(i, (Integer) entry.getValue());
                        }
                    }
                }
                components.put(DataComponentType.ENCHANTMENTS, new ItemEnchantments(enchantments, true));
            });
}
