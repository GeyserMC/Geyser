package org.geysermc.geyser.registry.populator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class DataComponentRegistryPopulator {
    public static final Int2ObjectMap<DataComponents> ITEM_COMPONENTS = new Int2ObjectOpenHashMap<>();

    private DataComponentRegistryPopulator() {
    }

    public static void populate() {
        ITEM_COMPONENTS.clear();

        try {
            JsonElement root = GeyserImpl.GSON.fromJson(
                    new java.io.InputStreamReader(
                            GeyserImpl.getInstance().getBootstrap().getResourceOrThrow("mappings/item_data_components.json"),
                            java.nio.charset.StandardCharsets.UTF_8
                    ),
                    JsonElement.class
            );
            for (JsonElement itemElement : root.getAsJsonArray()) {
                JsonObject itemEntry = itemElement.getAsJsonObject();
                int javaId = itemEntry.get("id").getAsInt();
                JsonObject componentsNode = itemEntry.getAsJsonObject("components");
                DataComponents components = new DataComponents(new HashMap<>());

                if (componentsNode != null) {
                    Iterator<Map.Entry<String, JsonElement>> componentIterator = componentsNode.entrySet().iterator();
                    while (componentIterator.hasNext()) {
                        Map.Entry<String, JsonElement> componentEntry = componentIterator.next();
                        try {
                            JsonElement encodedNode = componentEntry.getValue();
                            if (encodedNode == null || encodedNode.isJsonNull() || !encodedNode.isJsonPrimitive() || !encodedNode.getAsJsonPrimitive().isString()) {
                                continue;
                            }

                            byte[] bytes = Base64.getDecoder().decode(encodedNode.getAsString());
                            ByteBuf buf = Unpooled.wrappedBuffer(bytes);
                            try {
                                int typeId = MinecraftTypes.readVarInt(buf);
                                DataComponentType<?> dataComponentType = DataComponentTypes.from(typeId);
                                if (dataComponentType == null) {
                                    GeyserImpl.getInstance().getLogger().warning("Skipping unknown data component type " + typeId
                                            + " for Java item " + javaId + " (" + componentEntry.getKey() + ").");
                                    continue;
                                }

                                Object value = dataComponentType.readDataComponent(buf).getValue();
                                if (value != null) {
                                    putComponent(components, dataComponentType, value);
                                }
                            } finally {
                                buf.release();
                            }
                        } catch (Exception componentException) {
                            GeyserImpl.getInstance().getLogger().warning("Skipping malformed data component " + componentEntry.getKey()
                                    + " for Java item " + javaId + ": " + componentException);
                            // TEMP DEBUG: print full cause chain so we can see exactly where the reader fails
                            Throwable t = componentException;
                            while (t != null) {
                                GeyserImpl.getInstance().getLogger().warning("  caused by: " + t.getClass().getName() + ": " + t.getMessage());
                                StackTraceElement[] trace = t.getStackTrace();
                                for (int i = 0; i < Math.min(6, trace.length); i++) {
                                    GeyserImpl.getInstance().getLogger().warning("      at " + trace[i]);
                                }
                                t = t.getCause();
                            }
                        }
                    }
                }

                ITEM_COMPONENTS.put(javaId, components);
            }
        } catch (Exception e) {
            throw new AssertionError("Unable to load or parse components", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void putComponent(DataComponents components, DataComponentType<?> dataComponentType, Object value) {
        components.put((DataComponentType) dataComponentType, value);
    }
}
