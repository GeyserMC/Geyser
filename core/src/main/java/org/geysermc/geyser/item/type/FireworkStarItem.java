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

import com.github.steveice10.mc.protocol.data.game.item.component.DataComponentPatch;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;

public class FireworkStarItem extends Item {
    public FireworkStarItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponentPatch components, @NonNull NbtMapBuilder builder) {
        super.translateComponentsToBedrock(session, components, builder);

        Tag explosion = tag.remove("Explosion");
        if (explosion instanceof CompoundTag) {
            CompoundTag newExplosion = FireworkRocketItem.translateExplosionToBedrock((CompoundTag) explosion, "FireworksItem");
            tag.put(newExplosion);
            Tag color = ((CompoundTag) explosion).get("Colors");
            if (color instanceof IntArrayTag) {
                // Determine the custom color, if any.
                // Mostly replicates Java's own rendering code, as Java determines the final firework star color client-side
                // while Bedrock determines it server-side.
                int[] colors = ((IntArrayTag) color).getValue();
                if (colors.length == 0) {
                    return;
                }
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

                tag.put(new IntTag("customColor", finalColor));
            }
        }
    }

    @Override
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(tag, mapping);

        Tag explosion = tag.remove("FireworksItem");
        if (explosion instanceof CompoundTag) {
            CompoundTag newExplosion = FireworkRocketItem.translateExplosionToJava((CompoundTag) explosion, "Explosion");
            tag.put(newExplosion);
        }
        // Remove custom color, if any, since this only exists on Bedrock
        tag.remove("customColor");
    }
}
