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
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOverloadData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParam;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamData;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@ToString
public class ParamInfo {
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

    private final CommandNode paramNode;
    private final CommandParamData paramData;
    private final List<ParamInfo> children;

    /**
     * Create a new parameter info object
     *
     * @param paramNode CommandNode the parameter is for
     * @param paramData The existing parameters for the command
     */
    public ParamInfo(CommandNode paramNode, CommandParamData paramData) {
        this.paramNode = paramNode;
        this.paramData = paramData;
        this.children = new ArrayList<>();
    }

    /**
     * Convert Java edition command types to Bedrock edition
     *
     * @param data the {@link CommandTranslationData}
     * @param node Command type to convert
     * @return Bedrock parameter data type
     */
    private static Object mapCommandType(CommandTranslationData data, CommandNode node) {
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
            case RESOURCE_OR_TAG, RESOURCE_OR_TAG_KEY -> handleResource(data, ((ResourceProperties) node.getProperties()).getRegistryKey(), true);
            case RESOURCE, RESOURCE_KEY -> handleResource(data, ((ResourceProperties) node.getProperties()).getRegistryKey(), false);
            case TEMPLATE_MIRROR -> MIRROR_MODES;
            case TEMPLATE_ROTATION -> ROTATION_MODES;
            case HEIGHTMAP -> HEIGHTMAPS;
            // These are reloadable registries and as such can never be loaded by Geyser
            case LOOT_TABLE, LOOT_PREDICATE, LOOT_MODIFIER, FEATURE,
                 CONTEXT_FLOAT_PROVIDER, CONTEXT_INT_PROVIDER -> CommandParam.FILE_PATH;
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

    /**
     * Build the array of all the child parameters (recursive)
     *
     * @param data the {@link CommandTranslationData}
     */
    public void buildChildren(CommandTranslationData data) {
        for (int paramID : paramNode.getChildIndices()) {
            CommandNode paramNode = data.nodes()[paramID];

            if (paramNode == this.paramNode) {
                // Fixes a StackOverflowError when an argument has itself as a child
                continue;
            }

            if (paramNode.getParser() == null) {
                boolean foundCompatible = false;
                for (int i = 0; i < children.size(); i++) {
                    ParamInfo enumParamInfo = children.get(i);
                    // Check to make sure all descending nodes of this command are compatible - otherwise, create a new overload
                    if (isCompatible(data.nodes(), enumParamInfo.getParamNode(), paramNode)) {
                        foundCompatible = true;
                        // TODO: Check this
                        // Extend the current list of enum values
                        // String[] enumOptions = Arrays.copyOf(enumParamInfo.getParamData().getEnumData().getValues(), enumParamInfo.getParamData().getEnumData().getValues().size() + 1);
                        // enumOptions[enumOptions.length - 1] = paramNode.getName();

                        Map<String, Set<CommandEnumConstraint>> values = new LinkedHashMap<>(enumParamInfo.getParamData().getEnumData().getValues());
                        values.put(paramNode.getName(), Set.of());

                        // Re-create the command using the updated values
                        boolean soft = enumParamInfo.getParamData().getEnumData().isSoft() || needsSoftEnum(paramNode.getName());
                        String enumName = soft
                            ? softEnumName(data, enumParamInfo.getParamData().getName(), values)
                            : enumParamInfo.getParamData().getEnumData().getName();
                        CommandEnumData enumData = new CommandEnumData(enumName, values, soft);
                        CommandParamData commandParamData = new CommandParamData();
                        commandParamData.setName(enumParamInfo.getParamData().getName());
                        commandParamData.setOptional(this.paramNode.isExecutable());
                        commandParamData.setEnumData(enumData);

                        children.set(i, new ParamInfo(enumParamInfo.getParamNode(), commandParamData));
                        break;
                    }
                }

                if (!foundCompatible) {
                    // Create a new subcommand with this exact type
                    LinkedHashMap<String, Set<CommandEnumConstraint>> map = new LinkedHashMap<>();
                    map.put(paramNode.getName(), Set.of());
                    boolean soft = needsSoftEnum(paramNode.getName());
                    CommandEnumData enumData = new CommandEnumData(
                        soft ? softEnumName(data, paramNode.getName(), map) : paramNode.getName(), map, soft);

                    // On setting optional:
                    // isExecutable is defined as a node "constitutes a valid command."
                    // Therefore, any children of the parameter must simply be optional.
                    CommandParamData commandParamData = new CommandParamData();
                    commandParamData.setName(paramNode.getName());
                    commandParamData.setOptional(this.paramNode.isExecutable());
                    commandParamData.setEnumData(enumData);

                    children.add(new ParamInfo(paramNode, commandParamData));
                }
            } else {
                // Put the non-enum param into the list
                Object mappedType = mapCommandType(data, paramNode);
                CommandEnumData enumData = null;
                CommandParam type = null;
                boolean optional = this.paramNode.isExecutable();
                if (mappedType instanceof CommandEnumData) {
                    // Likely to specify isSoft, to be possibly updated later.
                    enumData = (CommandEnumData) mappedType;
                } else if (mappedType instanceof String[]) {
                    LinkedHashMap<String, Set<CommandEnumConstraint>> map = new LinkedHashMap<>();
                    for (String s : (String[]) mappedType) {
                        map.put(s, Set.of());
                    }

                    enumData = new CommandEnumData(getEnumDataName(paramNode).toLowerCase(Locale.ROOT), map, false);
                } else {
                    type = (CommandParam) mappedType;
                    // Bedrock throws a fit if an optional message comes after a string or target
                    // Example vanilla commands: ban-ip, ban, and kick
                    if (optional && type == CommandParam.MESSAGE && paramData != null && (paramData.getType() == CommandParam.STRING || paramData.getType() == CommandParam.TARGET)) {
                        optional = false;
                    }
                }
                // IF enumData != null:
                // In game, this will show up like <paramNode.getName(): enumData.getName()>
                // So if paramNode.getName() == "value" and enumData.getName() == "bool": <value: bool>
                CommandParamData commandParamData = new CommandParamData();
                commandParamData.setName(paramNode.getName());
                commandParamData.setOptional(optional);
                commandParamData.setEnumData(enumData);
                commandParamData.setType(type);

                children.add(new ParamInfo(paramNode, commandParamData));
            }
        }

        // Recursively build all child options
        for (ParamInfo child : children) {
            child.buildChildren(data);
        }
    }

    /**
     * Bedrock rejects fixed enum values that are not lowercase ("Keywords in commands need to be
     * lower case"), while soft enum values keep their case. Brigadier literals are case-sensitive,
     * so the exact spelling has to survive the round trip. Vanilla BDS uses the same split.
     */
    private static boolean needsSoftEnum(String literal) {
        return !literal.equals(literal.toLowerCase(Locale.ROOT));
    }

    /**
     * Soft enums are registered globally by name on the client, while hard enum groups elsewhere
     * may reuse the same first-literal name. Interning the name by the exact value set keeps it
     * identical across command aliases, so they still deduplicate, and gives every distinct set
     * its own name. The client only displays the values.
     */
    private static String softEnumName(CommandTranslationData data, String literal, Map<String, Set<CommandEnumConstraint>> values) {
        String signature = values.keySet().stream().sorted().collect(Collectors.joining(" "));
        return data.softEnumNames().computeIfAbsent(signature, key -> literal + "_" + data.softEnumNames().size());
    }

    /**
     * Mitigates <a href="https://github.com/GeyserMC/Geyser/issues/3411">issue 3411</a>. Not a perfect solution.
     */
    private static String getEnumDataName(CommandNode node) {
        if (node.getProperties() instanceof ResourceProperties properties) {
            Key registryKey = properties.getRegistryKey();
            return registryKey.value();
        }
        return node.getParser().name();
    }

    /**
     * Comparing CommandNode type a and b, determine if they are in the same overload.
     * <p>
     * Take the <code>gamerule</code> command, and let's present three "subcommands" you can perform:
     *
     * <ul>
     *     <li><code>gamerule doDaylightCycle true</code></li>
     *     <li><code>gamerule announceAdvancements false</code></li>
     *     <li><code>gamerule randomTickSpeed 3</code></li>
     * </ul>
     * <p>
     * While all three of them are indeed part of the same command, the command setting randomTickSpeed parses an int,
     * while the others use boolean. In Bedrock, this should be presented as a separate overload to indicate that this
     * does something a little different.
     * <p>
     * Therefore, this function will return <code>true</code> if the first two are compared, as they use the same
     * parsers. If the third is compared with either of the others, this function will return <code>false</code>.
     * <p>
     * Here's an example of how the above would be presented to Bedrock (as of 1.16.200). Notice how the top two <code>CommandParamData</code>
     * classes of each array are identical in type, but the following class is different:
     * <pre>
     *     overloads=[
     *         [
     *            CommandParamData(name=doDaylightCycle, optional=false, enumData=CommandEnumData(name=announceAdvancements, values=[announceAdvancements, doDaylightCycle], isSoft=false), type=STRING, postfix=null, options=[])
     *            CommandParamData(name=value, optional=false, enumData=CommandEnumData(name=value, values=[true, false], isSoft=false), type=null, postfix=null, options=[])
     *         ]
     *         [
     *            CommandParamData(name=randomTickSpeed, optional=false, enumData=CommandEnumData(name=randomTickSpeed, values=[randomTickSpeed], isSoft=false), type=STRING, postfix=null, options=[])
     *            CommandParamData(name=value, optional=false, enumData=null, type=INT, postfix=null, options=[])
     *         ]
     *     ]
     * </pre>
     *
     * @return if these two can be merged into one overload.
     */
    private boolean isCompatible(CommandNode[] allNodes, CommandNode a, CommandNode b) {
        if (a == b) return true;
        if (a.getParser() != b.getParser()) return false;
        if (a.getChildIndices().length != b.getChildIndices().length) return false;

        for (int i = 0; i < a.getChildIndices().length; i++) {
            boolean hasSimilarity = false;
            CommandNode a1 = allNodes[a.getChildIndices()[i]];
            // Search "b" until we find a child that matches this one
            for (int j = 0; j < b.getChildIndices().length; j++) {
                if (isCompatible(allNodes, a1, allNodes[b.getChildIndices()[j]])) {
                    hasSimilarity = true;
                    break;
                }
            }

            if (!hasSimilarity) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the tree of every parameter node (recursive)
     *
     * @return List of parameter options arrays for the command
     */
    public List<CommandOverloadData> getTree() {
        List<CommandOverloadData> treeParamData = new ArrayList<>();

        for (ParamInfo child : children) {
            // Get the tree from the child
            List<CommandOverloadData> childTree = child.getTree();

            // Un-pack the tree append the child node to it and push into the list
            for (CommandOverloadData subChildData : childTree) {
                CommandParamData[] subChild = subChildData.getOverloads();
                CommandParamData[] tmpTree = new CommandParamData[subChild.length + 1];
                tmpTree[0] = child.getParamData();
                System.arraycopy(subChild, 0, tmpTree, 1, subChild.length);

                treeParamData.add(new CommandOverloadData(false, tmpTree));
            }

            // If we have no more child parameters just the child
            if (childTree.size() == 0) {
                treeParamData.add(new CommandOverloadData(false, new CommandParamData[]{child.getParamData()}));
            }
        }

        return treeParamData;
    }
}
