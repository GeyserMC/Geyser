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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.item.TooltipOptions;
import org.geysermc.geyser.level.FireworkColor;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Fireworks;

import java.util.ArrayList;
import java.util.List;

public class FireworkRocketItem extends Item implements BedrockRequiresTagItem {
    public FireworkRocketItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull TooltipOptions tooltip, @NonNull BedrockItemBuilder builder) {
        Fireworks fireworks = components.get(DataComponentTypes.FIREWORKS);
        if (fireworks == null) {
            return;
        }
        // We still need to translate the explosion so this is still correct, and can be reverse translate in translateNbtToJava.
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

        // If the tooltip is hidden, don't add any lore.
        if (!tooltip.showInTooltip(DataComponentTypes.FIREWORKS)) {
            return;
        }

        // Then we translate everything into lore since the explosion tag and everything is not visible anymore due to this being a data driven item.
        List<String> lore = builder.getOrCreateLore();
        lore.add(withTranslation("§r", "item.fireworks.flight", " " + fireworks.getFlightDuration()));

        for (Fireworks.FireworkExplosion explosion : explosions) {
            lore.add(withTranslation("§r  ", "item.fireworksCharge.type." + explosion.getShapeId()));

            final List<String> colors = new ArrayList<>();
            colors.add("§r  ");
            for (int color : explosion.getColors()) {
                FireworkColor fireworkColor = FireworkColor.values()[FireworkColor.fromJavaRGB(color)];
                colors.add("item.fireworksCharge." + fireworkColor.getName());
                colors.add(" ");
            }

            if (explosion.getColors().length != 0) {
                lore.add(withTranslation(colors.toArray(new String[0])));
            }

            colors.clear();
            colors.add("§r  ");
            colors.add("item.fireworksCharge.fadeTo");
            colors.add(" ");
            for (int color : explosion.getFadeColors()) {
                FireworkColor fireworkColor = FireworkColor.values()[FireworkColor.fromJavaRGB(color)];
                colors.add("item.fireworksCharge." + fireworkColor.getName());
                colors.add(" ");
            }
            if (explosion.getFadeColors().length != 0) {
                lore.add(withTranslation(colors.toArray(new String[0])));
            }

            if (explosion.isHasTrail()) {
                lore.add(withTranslation("§r  ", "item.fireworksCharge.trail"));
            }

            if (explosion.isHasTwinkle()) {
                lore.add(withTranslation("§r  ", "item.fireworksCharge.flicker"));
            }
        }
    }

    @Override
    public void translateNbtToJava(@NonNull GeyserSession session, @NonNull NbtMap bedrockTag, @NonNull DataComponents components, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(session, bedrockTag, components, mapping);

        NbtMap fireworksTag = bedrockTag.getCompound("Fireworks");
        if (!fireworksTag.isEmpty()) {
            int flightDuration = 1;
            if (fireworksTag.containsKey("Flight")) {
                flightDuration = fireworksTag.getByte("Flight");
            }

            List<NbtMap> explosions = fireworksTag.getList("Explosions", NbtType.COMPOUND);
            if (!explosions.isEmpty()) {
                List<Fireworks.FireworkExplosion> javaExplosions = new ArrayList<>();
                for (NbtMap explosion : explosions) {
                    Fireworks.FireworkExplosion javaExplosion = translateExplosionToJava(explosion);
                    if (javaExplosion != null) {
                        javaExplosions.add(javaExplosion);
                    }
                }
                components.put(DataComponentTypes.FIREWORKS, new Fireworks(flightDuration, javaExplosions));
            } else {
                components.put(DataComponentTypes.FIREWORKS, new Fireworks(flightDuration, List.of()));
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

    public static String withTranslation(String... strings) {
        final JsonObject object = new JsonObject();
        final JsonArray array = new JsonArray();

        for (String s : strings) {
            final JsonObject object1 = new JsonObject();
            object1.addProperty("translate", s);
            array.add(object1);
        }

        object.add("rawtext", array);
        return object.toString();
    }
}
