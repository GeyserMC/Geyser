package org.geysermc.geyser.registry.populator;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

public class Conversion649_630 {
    
        static GeyserMappingItem remapItem(@SuppressWarnings("unused") Item item, GeyserMappingItem mapping) {
            mapping = Conversion662_649.remapItem(item, mapping);

            String identifer = mapping.getBedrockIdentifier();

            switch (identifer) {
                case "minecraft:turtle_scute" -> { return mapping.withBedrockIdentifier("minecraft:scute"); }
                case "minecraft:trial_spawner" -> { return mapping.withBedrockIdentifier("minecraft:mob_spawner"); }
                case "minecraft:trial_key" -> { return mapping.withBedrockIdentifier("minecraft:echo_shard"); }
                default -> { return mapping; }
            }
        }
    
        static NbtMap remapBlock(NbtMap tag) {
            tag = Conversion662_649.remapBlock(tag);

            final String name = tag.getString("name");
    
            if (name.equals("minecraft:trial_spawner")) {
                NbtMapBuilder builder = tag.toBuilder()
                    .putString("name", "minecraft:mob_spawner")
                    .putCompound("states", NbtMap.EMPTY);
    
                return builder.build();
            }
    
            return tag;
        }
}
