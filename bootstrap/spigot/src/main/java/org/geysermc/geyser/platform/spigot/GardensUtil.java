package org.geysermc.geyser.platform.spigot;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.geysermc.geyser.registry.populator.conversion.FurnitureItemConverter;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.Nullable;

public class GardensUtil {

    @Nullable
    public static GeyserBedrockBlock getBedrockBlockOverride(GeyserSession session, int x, int y, int z) {
        var furnitureStore = getFurnitureStore(session, x, y, z);
        if (furnitureStore != null) {
            var customItem = GardensFurniture.itemRegistry.getItem(furnitureStore.getItemIdentifier());
            if (customItem instanceof CustomPlaceableItem placeableItem) {
                var block = Bukkit.getPlayer(session.getPlayerEntity().uuid()).getWorld().getBlockAt(x, y, z);
                return getGeyserBlock(session, placeableItem, block);
            }
        }
        return null;
    }

    @Nullable
    private static GeyserBedrockBlock getGeyserBlock(GeyserSession session, CustomPlaceableItem placeableItem, Block block) {
        var blockData = FurnitureItemConverter.CUSTOM_NAME_TO_BLOCK.get(placeableItem.getIdentifier());
        if (blockData == null) {
            return null;
        }
        var blockStateBuilder = blockData.blockStateBuilder();
        placeableItem.onBuildGeyserBlockState(blockStateBuilder, block);
        var b = blockStateBuilder.build();
        return session.getBlockMappings().getCustomBlockStateDefinitions().get(b);
    }

    private static FurnitureStore getFurnitureStore(GeyserSession session, int x, int y, int z) {
        var player = Bukkit.getPlayer(session.getPlayerEntity().uuid());
        if (player == null) {
            return null;
        }
        var world = player.getWorld();
        var chunk = world.getChunkAt(new Location(world, x, y, z));
        return FurnitureStoreHandler.INSTANCE.getFurnitureStore(chunk, x, y, z);
    }
}
