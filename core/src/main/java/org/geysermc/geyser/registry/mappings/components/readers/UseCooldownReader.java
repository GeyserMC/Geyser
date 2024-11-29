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
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.components.DataComponentReader;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseCooldown;

public class UseCooldownReader extends DataComponentReader<UseCooldown> {

    public UseCooldownReader() {
        super(DataComponentType.USE_COOLDOWN);
    }

    @Override
    protected UseCooldown readDataComponent(@NonNull JsonNode node) throws InvalidCustomMappingsFileException {
        requireObject(node);

        JsonNode seconds = node.get("seconds");
        JsonNode cooldown_group = node.get("cooldown_group");

        if (seconds == null || !seconds.isNumber()) {
            throw new InvalidCustomMappingsFileException("Expected seconds to be a number");
        }

        if (cooldown_group == null || !cooldown_group.isTextual()) {
            throw new InvalidCustomMappingsFileException("Expected cooldown group to be a resource location");
        }

        return new UseCooldown((float) seconds.asDouble(), Key.key(cooldown_group.asText()));
    }
}
