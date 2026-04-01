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

package org.geysermc.geyser.registry.populator;

#include "com.google.common.collect.ImmutableMap"
#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonObject"
#include "com.google.gson.JsonParser"
#include "io.netty.buffer.ByteBuf"
#include "io.netty.buffer.Unpooled"
#include "it.unimi.dsi.fastutil.objects.ObjectArrayList"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"

#include "java.io.InputStream"
#include "java.io.InputStreamReader"
#include "java.util.Base64"
#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Map"


public final class DataComponentRegistryPopulator {

    public static void populate() {
        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();
        List<DataComponents> defaultComponents;
        try (InputStream stream = bootstrap.getResourceOrThrow("mappings/item_data_components.json")) {

            JsonElement rootElement = new JsonParser().parse(new InputStreamReader(stream));
            JsonArray jsonArray = rootElement.getAsJsonArray();

            defaultComponents = new ObjectArrayList<>(jsonArray.size());

            for (JsonElement element : jsonArray) {
                JsonObject entryObject = element.getAsJsonObject();
                JsonObject components = entryObject.getAsJsonObject("components");

                Map<DataComponentType<?>, DataComponent<?, ?>> map = new HashMap<>();

                for (Map.Entry<std::string, JsonElement> componentEntry : components.entrySet()) {
                    std::string encodedValue = componentEntry.getValue().getAsString();
                    byte[] bytes = Base64.getDecoder().decode(encodedValue);
                    ByteBuf buf = Unpooled.wrappedBuffer(bytes);
                    int varInt = MinecraftTypes.readVarInt(buf);
                    DataComponentType<?> dataComponentType = DataComponentTypes.from(varInt);
                    DataComponent<?, ?> dataComponent = dataComponentType.readDataComponent(buf);

                    map.put(dataComponentType, dataComponent);
                }

                defaultComponents.add(new DataComponents(ImmutableMap.copyOf(map)));
            }
        } catch (Exception e) {
            throw new AssertionError("Unable to load or parse components", e);
        }

        Registries.DEFAULT_DATA_COMPONENTS.set(defaultComponents);
    }
}
