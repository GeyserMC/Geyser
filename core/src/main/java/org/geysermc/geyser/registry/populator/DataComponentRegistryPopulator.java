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

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemCodecHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DataComponentRegistryPopulator {

    public static void populate() {
        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();
        List<DataComponents> defaultComponents;
        try (InputStream stream = bootstrap.getResourceOrThrow("java/item_data_components.json")) {
            //noinspection deprecation - 1.16.5 breaks otherwise
            JsonElement rootElement = new JsonParser().parse(new InputStreamReader(stream));
            JsonArray jsonArray = rootElement.getAsJsonArray();

            defaultComponents = new ObjectArrayList<>(jsonArray.size());

            for (JsonElement element : jsonArray) {
                JsonObject entryObject = element.getAsJsonObject();
                JsonObject components = entryObject.getAsJsonObject("components");

                Map<DataComponentType<?>, DataComponent<?, ?>> map = new HashMap<>();

                for (Map.Entry<String, JsonElement> componentEntry : components.entrySet()) {
                    String encodedValue = componentEntry.getValue().getAsString();
                    byte[] bytes = Base64.getDecoder().decode(encodedValue);
                    ByteBuf buf = Unpooled.wrappedBuffer(bytes);
                    MinecraftCodecHelper helper = new MinecraftCodecHelper();
                    int varInt = helper.readVarInt(buf);
                    DataComponentType<?> dataComponentType = DataComponentType.from(varInt);
                    DataComponent<?, ?> dataComponent = dataComponentType.readDataComponent(ItemCodecHelper.INSTANCE, buf);

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
