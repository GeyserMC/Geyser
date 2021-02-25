/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.github.steveice10.opennbt.tag.builtin.ByteArrayTag;
import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import org.geysermc.connector.network.translators.item.NbtItemStackTranslator;
import org.geysermc.connector.utils.FireworkColor;
import org.geysermc.connector.utils.MathUtils;

/**
 * Stores common code for firework rockets and firework stars.
 */
public abstract class FireworkBaseTranslator extends NbtItemStackTranslator {

    protected CompoundTag translateExplosionToBedrock(CompoundTag explosion, String newName) {
        CompoundTag newExplosionData = new CompoundTag(newName);

        if (explosion.get("Type") != null) {
            newExplosionData.put(new ByteTag("FireworkType", MathUtils.convertByte(explosion.get("Type").getValue())));
        }

        if (explosion.get("Colors") != null) {
            int[] oldColors = (int[]) explosion.get("Colors").getValue();
            byte[] colors = new byte[oldColors.length];

            int i = 0;
            for (int color : oldColors) {
                colors[i++] = FireworkColor.fromJavaID(color).getBedrockID();
            }

            newExplosionData.put(new ByteArrayTag("FireworkColor", colors));
        }

        if (explosion.get("FadeColors") != null) {
            int[] oldColors = (int[]) explosion.get("FadeColors").getValue();
            byte[] colors = new byte[oldColors.length];

            int i = 0;
            for (int color : oldColors) {
                colors[i++] = FireworkColor.fromJavaID(color).getBedrockID();
            }

            newExplosionData.put(new ByteArrayTag("FireworkFade", colors));
        }

        if (explosion.get("Trail") != null) {
            newExplosionData.put(new ByteTag("FireworkTrail", MathUtils.convertByte(explosion.get("Trail").getValue())));
        }

        if (explosion.get("Flicker") != null) {
            newExplosionData.put(new ByteTag("FireworkFlicker", MathUtils.convertByte(explosion.get("Flicker").getValue())));
        }

        return newExplosionData;
    }

    protected CompoundTag translateExplosionToJava(CompoundTag explosion, String newName) {
        CompoundTag newExplosionData = new CompoundTag(newName);

        if (explosion.get("FireworkType") != null) {
            newExplosionData.put(new ByteTag("Type", MathUtils.convertByte(explosion.get("FireworkType").getValue())));
        }

        if (explosion.get("FireworkColor") != null) {
            byte[] oldColors = (byte[]) explosion.get("FireworkColor").getValue();
            int[] colors = new int[oldColors.length];

            int i = 0;
            for (byte color : oldColors) {
                colors[i++] = FireworkColor.fromBedrockID(color).getJavaID();
            }

            newExplosionData.put(new IntArrayTag("Colors", colors));
        }

        if (explosion.get("FireworkFade") != null) {
            byte[] oldColors = (byte[]) explosion.get("FireworkFade").getValue();
            int[] colors = new int[oldColors.length];

            int i = 0;
            for (byte color : oldColors) {
                colors[i++] = FireworkColor.fromBedrockID(color).getJavaID();
            }

            newExplosionData.put(new IntArrayTag("FadeColors", colors));
        }

        if (explosion.get("FireworkTrail") != null) {
            newExplosionData.put(new ByteTag("Trail", MathUtils.convertByte(explosion.get("FireworkTrail").getValue())));
        }

        if (explosion.get("FireworkFlicker") != null) {
            newExplosionData.put(new ByteTag("Flicker", MathUtils.convertByte(explosion.get("FireworkFlicker").getValue())));
        }

        return newExplosionData;
    }
}
