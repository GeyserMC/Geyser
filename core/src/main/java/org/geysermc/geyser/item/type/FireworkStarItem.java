/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Fireworks;

public class FireworkStarItem extends Item {
    public FireworkStarItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, builder);

        Fireworks.FireworkExplosion explosion = components.get(DataComponentType.FIREWORK_EXPLOSION);
        if (explosion != null) {
            NbtMap newExplosion = FireworkRocketItem.translateExplosionToBedrock(explosion);
            builder.putCompound("FireworksItem", newExplosion);
            int[] colors = explosion.getColors();
            if (colors.length != 0) {
                // Determine the custom color, if any.
                // Mostly replicates Java's own rendering code, as Java determines the final firework star color client-side
                // while Bedrock determines it server-side.
                int finalColor;
                if (colors.length == 1) {
                    finalColor = colors[0];
                } else {
                    int r = 0;
                    int g = 0;
                    int b = 0;

                    for (int fireworkColor : colors) {
                        r += (fireworkColor & (255 << 16)) >> 16;
                        g += (fireworkColor & (255 << 8)) >> 8;
                        b += fireworkColor & 255;
                    }

                    r /= colors.length;
                    g /= colors.length;
                    b /= colors.length;
                    finalColor = r << 16 | g << 8 | b;
                }

                builder.putInt("customColor", finalColor);
            }
        }
    }

    @Override
    public void translateNbtToJava(@NonNull GeyserSession session, @NonNull NbtMap bedrockTag, @NonNull DataComponents components, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(session, bedrockTag, components, mapping);

        NbtMap explosion = bedrockTag.getCompound("FireworksItem");
        if (!explosion.isEmpty()) {
            Fireworks.FireworkExplosion newExplosion = FireworkRocketItem.translateExplosionToJava(explosion);
            if (newExplosion == null) {
                return;
            }
            components.put(DataComponentType.FIREWORK_EXPLOSION, newExplosion);
        }
    }

    @Override
    public boolean ignoreDamage() {
        return true;
    }
}
