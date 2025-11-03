package org.geysermc.geyser.platform.spigot;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

public class GardensUtil {

    private static Map<String, GeyserBedrockBlock> CUSTOM_NAME_TO_BLOCK;

    public static int getBedrockBlockIdOverride(GeyserSession session, int x, int y, int z) {
        var furnitureStore = getFurnitureStore(session, x, y, z);
        if (furnitureStore != null) {
            var geyserBlock = getGeyserBlock(session, furnitureStore.getItemIdentifier());
            if (geyserBlock != null) {
                return geyserBlock.getRuntimeId();
            }
        }
        return -1;
    }

    @Nullable
    public static GeyserBedrockBlock getBedrockBlockOverride(GeyserSession session, int x, int y, int z) {
        var furnitureStore = getFurnitureStore(session, x, y, z);
        if (furnitureStore != null) {
            var customItem = GardensFurniture.itemRegistry.getItem(furnitureStore.getItemIdentifier());
            if (customItem instanceof CustomPlaceableItem placeableItem) {
                var block = Bukkit.getPlayer(session.getPlayerEntity().getUuid()).getWorld().getBlockAt(x, y, z);
                return getGeyserBlock(session, placeableItem.getPlacedBlock(block).getBlockId());
            }
        }
        return null;
    }

    @Nullable
    private static GeyserBedrockBlock getGeyserBlock(GeyserSession session, String blockId) {
        if (CUSTOM_NAME_TO_BLOCK == null) {
            CUSTOM_NAME_TO_BLOCK = session.getBlockMappings().getCustomBlockStateDefinitions().entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));
        }
        return CUSTOM_NAME_TO_BLOCK.get(blockId);
    }

    private static FurnitureStore getFurnitureStore(GeyserSession session, int x, int y, int z) {
        var player = Bukkit.getPlayer(session.getPlayerEntity().getUuid());
        if (player == null) {
            return null;
        }
        var world = player.getWorld();
        var chunk = world.getChunkAt(new Location(world, x, y, z));
        return FurnitureStoreHandler.INSTANCE.getFurnitureStore(chunk, x, y, z);
    }
}
