/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.type;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.item.TooltipOptions;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

public class ArmorItem extends Item {

    public ArmorItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull TooltipOptions tooltip, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        ArmorTrim trim = components.get(DataComponentTypes.TRIM);
        if (trim != null) {
            Key material;
            if (trim.material().isId()) {
                material = JavaRegistries.TRIM_MATERIAL.key(session, trim.material().id());
            } else {
                GeyserImpl.getInstance().getLogger().debug("Unable to translate non-id trim material: " + trim);
                return;
            }

            Key pattern;
            if (trim.pattern().isId()) {
                pattern = JavaRegistries.TRIM_PATTERN.key(session, trim.pattern().id());
            } else {
                GeyserImpl.getInstance().getLogger().debug("Unable to translate non-id trim pattern: " + trim);
                return;
            }

            if (material != null && pattern != null) {
                NbtMapBuilder trimBuilder = NbtMap.builder();
                // Strip namespace from identifiers - Bedrock expects just the path part
                // e.g., "minecraft:iron" -> "iron", "civilization:frost_trim" -> "frost_trim"
                trimBuilder.put("Material", material.value());
                trimBuilder.put("Pattern", pattern.value());
                builder.putCompound("Trim", trimBuilder.build());
            } else {
                GeyserImpl.getInstance().getLogger().debug("Unknown trim material/pattern: ", trim);
            }
        }
    }
}
