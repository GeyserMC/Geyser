/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.item.translators.nbt;

import com.github.steveice10.opennbt.tag.builtin.*;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.NbtItemStackTranslator;
import org.geysermc.connector.utils.FireworkColor;
import org.geysermc.connector.utils.MathUtils;

@ItemRemapper
public class FireworkTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(CompoundTag itemTag, ItemEntry itemEntry) {
        if (!itemTag.contains("Fireworks")) {
            return;
        }

        CompoundTag fireworks = itemTag.get("Fireworks");
        if (fireworks.get("Flight") != null) {
            fireworks.put(new ByteTag("Flight", MathUtils.convertByte(fireworks.get("Flight").getValue())));
        }

        ListTag explosions = fireworks.get("Explosions");
        if (explosions == null) {
            return;
        }
        for (Tag effect : explosions.getValue()) {
            CompoundTag effectData = (CompoundTag) effect;

            CompoundTag newEffectData = new CompoundTag("");

            if (effectData.get("Type") != null) {
                newEffectData.put(new ByteTag("FireworkType", MathUtils.convertByte(effectData.get("Type").getValue())));
            }

            if (effectData.get("Colors") != null) {
                int[] oldColors = (int[]) effectData.get("Colors").getValue();
                byte[] colors = new byte[oldColors.length];

                int i = 0;
                for (int color : oldColors) {
                    colors[i++] = FireworkColor.fromJavaID(color).getBedrockID();
                }

                newEffectData.put(new ByteArrayTag("FireworkColor", colors));
            }

            if (effectData.get("FadeColors") != null) {
                int[] oldColors = (int[]) effectData.get("FadeColors").getValue();
                byte[] colors = new byte[oldColors.length];

                int i = 0;
                for (int color : oldColors) {
                    colors[i++] = FireworkColor.fromJavaID(color).getBedrockID();
                }

                newEffectData.put(new ByteArrayTag("FireworkFade", colors));
            }

            if (effectData.get("Trail") != null) {
                newEffectData.put(new ByteTag("FireworkTrail", MathUtils.convertByte(effectData.get("Trail").getValue())));
            }

            if (effectData.get("Flicker") != null) {
                newEffectData.put(new ByteTag("FireworkFlicker", MathUtils.convertByte(effectData.get("Flicker").getValue())));
            }

            explosions.remove(effect);
            explosions.add(newEffectData);
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemEntry itemEntry) {
        if (!itemTag.contains("Fireworks")) {
            return;
        }
        CompoundTag fireworks = itemTag.get("Fireworks");
        if (fireworks.contains("Flight")) {
            fireworks.put(new ByteTag("Flight", MathUtils.convertByte(fireworks.get("Flight").getValue())));
        }

        ListTag explosions = fireworks.get("Explosions");
        for (Tag effect : explosions.getValue()) {
            CompoundTag effectData = (CompoundTag) effect;

            CompoundTag newEffectData = new CompoundTag("");

            if (effectData.get("FireworkType") != null) {
                newEffectData.put(new ByteTag("Type", MathUtils.convertByte(effectData.get("FireworkType").getValue())));
            }

            if (effectData.get("FireworkColor") != null) {
                byte[] oldColors = (byte[]) effectData.get("FireworkColor").getValue();
                int[] colors = new int[oldColors.length];

                int i = 0;
                for (byte color : oldColors) {
                    colors[i++] = FireworkColor.fromBedrockID(color).getJavaID();
                }

                newEffectData.put(new IntArrayTag("Colors", colors));
            }

            if (effectData.get("FireworkFade") != null) {
                byte[] oldColors = (byte[]) effectData.get("FireworkFade").getValue();
                int[] colors = new int[oldColors.length];

                int i = 0;
                for (byte color : oldColors) {
                    colors[i++] = FireworkColor.fromBedrockID(color).getJavaID();
                }

                newEffectData.put(new IntArrayTag("FadeColors", colors));
            }

            if (effectData.get("FireworkTrail") != null) {
                newEffectData.put(new ByteTag("Trail", MathUtils.convertByte(effectData.get("FireworkTrail").getValue())));
            }

            if (effectData.get("FireworkFlicker") != null) {
                newEffectData.put(new ByteTag("Flicker", MathUtils.convertByte(effectData.get("FireworkFlicker").getValue())));
            }

            explosions.remove(effect);
            explosions.add(newEffectData);
        }
    }

    @Override
    public boolean acceptItem(ItemEntry itemEntry) {
        return "minecraft:firework_rocket".equals(itemEntry.getJavaIdentifier());
    }
}
