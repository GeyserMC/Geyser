/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.command;

import com.google.common.base.Suppliers;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.SwingAnimation;
import org.geysermc.mcprotocollib.protocol.data.game.level.HeightmapTypes;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class BuiltInCommandEnums {
    private static final Supplier<String[]> BLOCK_NAMES = lazyRegistryContents(BuiltInJavaRegistries.BLOCK);
    private static final Supplier<String[]> ITEM_NAMES = lazyRegistryContents(BuiltInJavaRegistries.ITEM);
    private static final String[] COLOR_NAMES = NamedTextColor.NAMES.keys().toArray(new String[0]);
    private static final String[] PARTICLE_NAMES = Arrays.stream(ParticleType.values()).map(type -> "minecraft:" + type.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
    private static final String[] SCOREBOARD_SLOTS_NAMES;
    private static final String[] GAMEMODE_NAMES = Arrays.stream(GameMode.values()).map(mode -> mode.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
    private static final String[] HEIGHTMAP_NAMES = Arrays.stream(HeightmapTypes.values())
        // Java doesn't allow worldgen heightmaps
        .filter(type -> !type.isWorldGen())
        .map(type -> type.name().toLowerCase(Locale.ROOT))
        .toArray(String[]::new);
    private static final String[] SWING_ANIMATION_NAMES = Arrays.stream(SwingAnimation.Type.values()).map(type -> type.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);

    // Ordered by Java parser ID: see the switch in CommandParserTranslator
    static final Key BOOLEAN = key("boolean");
    static final Key BLOCKS = key("block");
    static final Key ITEMS = key("item");
    static final Key COLORS = key("color");
    static final Key PARTICLES = key("particle");
    static final Key SCOREBOARD_SLOTS = key("slot");
    static final Key TEAMS = key("team");
    static final Key ANCHORS = key("anchor");
    static final Key LEVELS = key("dimension");
    static final Key GAMEMODES = key("gamemode");
    static final Key MIRROR_MODES = key("mirror");
    static final Key ROTATION_MODES = key("rotation");
    static final Key HEIGHTMAPS = key("heightmap");
    static final Key SWING_ANIMATIONS = key("animation");

    private static Key key(String name) {
        return new Key(name);
    }

    private static Supplier<String[]> lazyRegistryContents(JavaRegistry<?> registry) {
        return Suppliers.memoize(() -> registry.keys().stream().map(net.kyori.adventure.key.Key::asString).toArray(String[]::new));
    }

    static {
        List<String> teamOptions = new ArrayList<>(Arrays.asList("list", "sidebar", "belowName"));
        for (String color : NamedTextColor.NAMES.keys()) {
            teamOptions.add("sidebar.team." + color);
        }
        SCOREBOARD_SLOTS_NAMES = teamOptions.toArray(String[]::new);
    }

    private BuiltInCommandEnums() {}

    public static void register(Registry registry) {
        // Bedrock doesn't have a hardcoded boolean param for some reason
        registry.accept(BOOLEAN, "true", "false");
        registry.accept(BLOCKS, BLOCK_NAMES.get());
        registry.accept(ITEMS, ITEM_NAMES.get());
        registry.accept(COLORS, COLOR_NAMES);
        registry.accept(PARTICLES, PARTICLE_NAMES);
        registry.accept(SCOREBOARD_SLOTS, SCOREBOARD_SLOTS_NAMES);
        registry.accept(TEAMS, registry.teams());
        // https://mcsrc.dev/2/26.3/net/minecraft/commands/arguments/EntityAnchorArgument
        // Feet first.
        registry.accept(ANCHORS, "feet", "eyes");
        registry.accept(LEVELS, registry.levels());
        registry.accept(GAMEMODES, GAMEMODE_NAMES);
        // https://mcsrc.dev/2/26.3/net/minecraft/world/level/block/Mirror
        registry.accept(MIRROR_MODES, "none", "left_right", "front_back");
        // https://mcsrc.dev/2/26.3/net/minecraft/world/level/block/Rotation
        registry.accept(ROTATION_MODES, "none", "clockwise_90", "180", "counterclockwise_90");
        registry.accept(HEIGHTMAPS, HEIGHTMAP_NAMES);
        registry.accept(SWING_ANIMATIONS, SWING_ANIMATION_NAMES);
    }

    public record Key(String name) {}

    public interface Registry {

        void accept(Key key, String... values);

        String[] teams();

        String[] levels();
    }
}
