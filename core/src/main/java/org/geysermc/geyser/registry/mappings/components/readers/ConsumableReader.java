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

package org.geysermc.geyser.registry.mappings.components.readers;

import com.fasterxml.jackson.databind.JsonNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.components.DataComponentReader;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.List;

public class ConsumableReader extends DataComponentReader<Consumable> {

    public ConsumableReader() {
        super(DataComponentType.CONSUMABLE);
    }

    @Override
    protected Consumable readDataComponent(@NonNull JsonNode node) throws InvalidCustomMappingsFileException {
        if (!node.isObject()) {
            throw new InvalidCustomMappingsFileException("Expected consumable component to be an object");
        }

        float consumeSeconds = 1.6F;
        if (node.has("consume_seconds")) {
            consumeSeconds = (float) node.get("consume_seconds").asDouble();
        }

        Consumable.ItemUseAnimation animation = Consumable.ItemUseAnimation.EAT;
        if (node.has("animation")) {
            animation = Consumable.ItemUseAnimation.valueOf(node.get("animation").asText().toUpperCase()); // TODO maybe not rely on the enum
        }

        return new Consumable(consumeSeconds, animation, BuiltinSound.ENTITY_GENERIC_EAT, false, List.of()); // TODO
    }
}
