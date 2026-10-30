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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParam;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.translator.data.CommandTranslationData;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandParser;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.ResourceProperties;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.SwingAnimation;
import org.geysermc.mcprotocollib.protocol.data.game.level.HeightmapTypes;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

final class CommandParserTranslator {
    /**
     * Wait until the registries load before getting all the block names.
     */
    private static final Supplier<String[]> ALL_BLOCK_NAMES = Suppliers.memoize(() -> BuiltInJavaRegistries.BLOCK.keys().stream().map(Key::asString).toArray(String[]::new));
    private static final String[] ALL_EFFECT_IDENTIFIERS = EntityUtils.getAllEffectIdentifiers();
    private static final String[] ATTRIBUTES = AttributeType.Builtin.BUILTIN.values().stream().map(type -> type.getIdentifier().asString()).toList().toArray(String[]::new);
    private static final String[] PARTICLES = Arrays.stream(ParticleType.values()).map(type -> "minecraft:" + type.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
    private static final String[] GAMEMODES = Arrays.stream(GameMode.values()).map(mode -> mode.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
    // https://mcsrc.dev/2/26.3/net/minecraft/commands/arguments/EntityAnchorArgument
    private static final String[] ANCHORS = {"eyes", "feet"};
    // https://mcsrc.dev/2/26.3/net/minecraft/world/level/block/Mirror
    private static final String[] MIRROR_MODES = {"none", "left_right", "front_back"};
    // https://mcsrc.dev/2/26.3/net/minecraft/world/level/block/Rotation
    private static final String[] ROTATION_MODES = {"none", "clockwise_90", "180", "counterclockwise_90"};
    private static final String[] HEIGHTMAPS = Arrays.stream(HeightmapTypes.values())
        // Java doesn't allow worldgen heightmaps
        .filter(type -> !type.isWorldGen())
        .map(type -> type.name().toLowerCase(Locale.ROOT))
        .toArray(String[]::new);
    private static final String[] SWING_ANIMATIONS = Arrays.stream(SwingAnimation.Type.values()).map(type -> type.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
    private static final String[] ENUM_BOOLEAN = {"true", "false"};
    private static final String[] VALID_COLORS = NamedTextColor.NAMES.keys().toArray(new String[0]);
    private static final String[] VALID_SCOREBOARD_SLOTS;

    static {
        List<String> teamOptions = new ArrayList<>(Arrays.asList("list", "sidebar", "belowName"));
        for (String color : NamedTextColor.NAMES.keys()) {
            teamOptions.add("sidebar.team." + color);
        }
        VALID_SCOREBOARD_SLOTS = teamOptions.toArray(new String[0]);
    }

    /**
     * Convert Java edition command types to Bedrock edition
     *
     * @param data the {@link CommandTranslationData}
     * @param node Command type to convert
     * @return Bedrock parameter data type
     */
    static Object mapCommandType(CommandTranslationData data, CommandNode node) {
        CommandParser parser = node.getParser();
        if (parser == null) {
            return CommandParam.STRING;
        }

        return switch (parser) {
            case BOOL -> ENUM_BOOLEAN;
            case FLOAT, DOUBLE, ROTATION -> CommandParam.FLOAT;
            case INTEGER, LONG -> CommandParam.INT;
            case ENTITY, GAME_PROFILE, SCORE_HOLDER -> CommandParam.TARGET;
            case BLOCK_POS -> CommandParam.BLOCK_POSITION;
            case COLUMN_POS, VEC3 -> CommandParam.POSITION;
            case BLOCK_STATE -> ALL_BLOCK_NAMES.get();
            case ITEM_STACK -> data.getRegistryElements(JavaRegistries.ITEM);
            case TEAM_COLOR -> VALID_COLORS;
            case COMPONENT -> CommandParam.TEXT;
            case MESSAGE -> CommandParam.MESSAGE;
            case NBT_COMPOUND_TAG, NBT_TAG, NBT_PATH -> CommandParam.JSON; //TODO NBT was removed
            case OPERATION -> CommandParam.OPERATOR; // ">=", "==", etc
            case PARTICLE -> PARTICLES;
            case SCOREBOARD_SLOT -> VALID_SCOREBOARD_SLOTS;
            case TEAM -> data.teams(); // Note: as of Java 1.19.3, objectives are currently parsed from the server
            case ITEM_SLOT, ITEM_SLOTS, SLOT_SOURCE -> CommandParam.EQUIPMENT_SLOTS;
            case RESOURCE_LOCATION, FUNCTION -> CommandParam.FILE_PATH; // TODO FIXME
            case ENTITY_ANCHOR -> ANCHORS;
            case INT_RANGE -> CommandParam.INT_RANGE_FULL;
            case FLOAT_RANGE -> CommandParam.RATIONAL_RANGE_FULL;
            case DIMENSION -> data.levels();
            case GAMEMODE -> GAMEMODES;
            // The _KEY variants are practically the same, but handled differently server-side
            case RESOURCE_OR_TAG, RESOURCE_OR_TAG_KEY -> handleResource(data, ((ResourceProperties) node.getProperties()).getRegistryKey(), true);
            case RESOURCE, RESOURCE_KEY -> handleResource(data, ((ResourceProperties) node.getProperties()).getRegistryKey(), false);
            case TEMPLATE_MIRROR -> MIRROR_MODES;
            case TEMPLATE_ROTATION -> ROTATION_MODES;
            case HEIGHTMAP -> HEIGHTMAPS;
            // These are reloadable registries and as such can never be loaded by Geyser
            case LOOT_TABLE, LOOT_PREDICATE, LOOT_MODIFIER, FEATURE,
                 CONTEXT_FLOAT_PROVIDER, CONTEXT_INT_PROVIDER -> CommandParam.FILE_PATH;
            // This one is separate from RESOURCE* parsers because it also allows inline dialogs
            case DIALOG -> data.getRegistryElements(JavaRegistries.DIALOG);
            case SWING_ANIMATION -> SWING_ANIMATIONS;
            default -> CommandParam.STRING;
        };
    }

    private static Object handleResource(CommandTranslationData data, Key registryKey, boolean tags) {
        // If the registry is tracked by us, this will be an easy case
        JavaRegistryKey<?> registry = JavaRegistries.fromKey(registryKey);
        if (registry != null) {
            return tags ? data.getRegistryElementsAndTags(registry) : data.getRegistryElements(registry);
        }

        // Else, go through some hardcoded registries:
        return switch (registryKey.asString()) {
            case "minecraft:attribute" -> ATTRIBUTES;
            case "minecraft:mob_effect" -> ALL_EFFECT_IDENTIFIERS;
            default -> CommandParam.STRING;
        };
    }

    private CommandParserTranslator() {}
}
