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

import it.unimi.dsi.fastutil.ints.IntArrays;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.level.FireworkColor;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Fireworks;

import java.util.ArrayList;
import java.util.List;

public class FireworkRocketItem extends Item implements BedrockRequiresTagItem {
    public FireworkRocketItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, builder);

        Fireworks fireworks = components.get(DataComponentType.FIREWORKS);
        if (fireworks == null) {
            return;
        }
        NbtMapBuilder fireworksNbt = NbtMap.builder();
        fireworksNbt.putByte("Flight", (byte) fireworks.getFlightDuration());

        List<Fireworks.FireworkExplosion> explosions = fireworks.getExplosions();
        if (!explosions.isEmpty()) {
            List<NbtMap> explosionNbt = new ArrayList<>();
            for (Fireworks.FireworkExplosion explosion : explosions) {
                explosionNbt.add(translateExplosionToBedrock(explosion));
            }
            fireworksNbt.putList("Explosions", NbtType.COMPOUND, explosionNbt);
        } else {
            // This is the default firework
            fireworksNbt.put("Explosions", NbtList.EMPTY);
        }
        builder.putCompound("Fireworks", fireworksNbt.build());
    }

    @Override
    public void translateNbtToJava(@NonNull GeyserSession session, @NonNull NbtMap bedrockTag, @NonNull DataComponents components, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(session, bedrockTag, components, mapping);

        NbtMap fireworksTag = bedrockTag.getCompound("Fireworks");
        if (!fireworksTag.isEmpty()) {
            List<NbtMap> explosions = fireworksTag.getList("Explosions", NbtType.COMPOUND);
            if (!explosions.isEmpty()) {
                List<Fireworks.FireworkExplosion> javaExplosions = new ArrayList<>();
                for (NbtMap explosion : explosions) {
                    Fireworks.FireworkExplosion javaExplosion = translateExplosionToJava(explosion);
                    if (javaExplosion != null) {
                        javaExplosions.add(javaExplosion);
                    }
                }
                components.put(DataComponentType.FIREWORKS, new Fireworks(1, javaExplosions));
            }
        }
    }

    static NbtMap translateExplosionToBedrock(Fireworks.FireworkExplosion explosion) {
        NbtMapBuilder newExplosionData = NbtMap.builder();

        newExplosionData.putByte("FireworkType", (byte) explosion.getShapeId());

        int[] oldColors = explosion.getColors();
        byte[] colors = new byte[oldColors.length];

        int i = 0;
        for (int color : oldColors) {
            colors[i++] = FireworkColor.fromJavaRGB(color);
        }

        newExplosionData.putByteArray("FireworkColor", colors);

        oldColors = explosion.getFadeColors();
        colors = new byte[oldColors.length];

        i = 0;
        for (int color : oldColors) {
            colors[i++] = FireworkColor.fromJavaRGB(color);
        }

        newExplosionData.putByteArray("FireworkFade", colors);

        newExplosionData.putBoolean("FireworkTrail", explosion.isHasTrail());
        newExplosionData.putBoolean("FireworkFlicker", explosion.isHasTwinkle());

        return newExplosionData.build();
    }

    /**
     * The only thing that the Bedrock creative inventory has - as of 1.20.80 - is color.
     */
    static Fireworks.FireworkExplosion translateExplosionToJava(NbtMap explosion) {
        byte[] javaColors = explosion.getByteArray("FireworkColor", null);
        if (javaColors != null) {
            int[] colors = new int[javaColors.length];

            int i = 0;
            for (byte color : javaColors) {
                colors[i++] = FireworkColor.fromBedrockId(color);
            }

            return new Fireworks.FireworkExplosion(0, colors, IntArrays.EMPTY_ARRAY, false, false);
        } else {
            return null;
        }
    }
}
