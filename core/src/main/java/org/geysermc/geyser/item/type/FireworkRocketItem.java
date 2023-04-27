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

import com.github.steveice10.opennbt.tag.builtin.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.level.FireworkColor;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MathUtils;

public class FireworkRocketItem extends Item {
    public FireworkRocketItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        CompoundTag fireworks = tag.get("Fireworks");
        if (fireworks == null) {
            return;
        }

        if (fireworks.get("Flight") != null) {
            fireworks.put(new ByteTag("Flight", MathUtils.getNbtByte(fireworks.get("Flight").getValue())));
        }

        ListTag explosions = fireworks.get("Explosions");
        if (explosions == null) {
            return;
        }
        for (Tag effect : explosions.getValue()) {
            CompoundTag effectData = (CompoundTag) effect;
            CompoundTag newEffectData = translateExplosionToBedrock(effectData, "");

            explosions.remove(effectData);
            explosions.add(newEffectData);
        }
    }

    @Override
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(tag, mapping);
    }

    static CompoundTag translateExplosionToBedrock(CompoundTag explosion, String newName) {
        CompoundTag newExplosionData = new CompoundTag(newName);

        if (explosion.get("Type") != null) {
            newExplosionData.put(new ByteTag("FireworkType", MathUtils.getNbtByte(explosion.get("Type").getValue())));
        }

        if (explosion.get("Colors") != null) {
            int[] oldColors = (int[]) explosion.get("Colors").getValue();
            byte[] colors = new byte[oldColors.length];

            int i = 0;
            for (int color : oldColors) {
                colors[i++] = FireworkColor.fromJavaRGB(color);
            }

            newExplosionData.put(new ByteArrayTag("FireworkColor", colors));
        }

        if (explosion.get("FadeColors") != null) {
            int[] oldColors = (int[]) explosion.get("FadeColors").getValue();
            byte[] colors = new byte[oldColors.length];

            int i = 0;
            for (int color : oldColors) {
                colors[i++] = FireworkColor.fromJavaRGB(color);
            }

            newExplosionData.put(new ByteArrayTag("FireworkFade", colors));
        }

        if (explosion.get("Trail") != null) {
            newExplosionData.put(new ByteTag("FireworkTrail", MathUtils.getNbtByte(explosion.get("Trail").getValue())));
        }

        if (explosion.get("Flicker") != null) {
            newExplosionData.put(new ByteTag("FireworkFlicker", MathUtils.getNbtByte(explosion.get("Flicker").getValue())));
        }

        return newExplosionData;
    }

    static CompoundTag translateExplosionToJava(CompoundTag explosion, String newName) {
        CompoundTag newExplosionData = new CompoundTag(newName);

        if (explosion.get("FireworkType") != null) {
            newExplosionData.put(new ByteTag("Type", MathUtils.getNbtByte(explosion.get("FireworkType").getValue())));
        }

        if (explosion.get("FireworkColor") != null) {
            byte[] oldColors = (byte[]) explosion.get("FireworkColor").getValue();
            int[] colors = new int[oldColors.length];

            int i = 0;
            for (byte color : oldColors) {
                colors[i++] = FireworkColor.fromBedrockId(color);
            }

            newExplosionData.put(new IntArrayTag("Colors", colors));
        }

        if (explosion.get("FireworkFade") != null) {
            byte[] oldColors = (byte[]) explosion.get("FireworkFade").getValue();
            int[] colors = new int[oldColors.length];

            int i = 0;
            for (byte color : oldColors) {
                colors[i++] = FireworkColor.fromBedrockId(color);
            }

            newExplosionData.put(new IntArrayTag("FadeColors", colors));
        }

        if (explosion.get("FireworkTrail") != null) {
            newExplosionData.put(new ByteTag("Trail", MathUtils.getNbtByte(explosion.get("FireworkTrail").getValue())));
        }

        if (explosion.get("FireworkFlicker") != null) {
            newExplosionData.put(new ByteTag("Flicker", MathUtils.getNbtByte(explosion.get("FireworkFlicker").getValue())));
        }

        return newExplosionData;
    }
}
