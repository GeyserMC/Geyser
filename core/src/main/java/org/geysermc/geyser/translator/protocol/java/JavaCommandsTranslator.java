/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.protocol.data.game.command.CommandNode;
import com.github.steveice10.mc.protocol.data.game.command.CommandParser;
import com.github.steveice10.mc.protocol.data.game.command.properties.ResourceProperties;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import com.nukkitx.protocol.bedrock.data.command.CommandData;
import com.nukkitx.protocol.bedrock.data.command.CommandEnumData;
import com.nukkitx.protocol.bedrock.data.command.CommandParam;
import com.nukkitx.protocol.bedrock.data.command.CommandParamData;
import com.nukkitx.protocol.bedrock.packet.AvailableCommandsPacket;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.downstream.ServerDefineCommandsEvent;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.inventory.item.Enchantment;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.EntityUtils;

import java.util.*;

@Translator(packet = ClientboundCommandsPacket.class)
public class JavaCommandsTranslator extends PacketTranslator<ClientboundCommandsPacket> {

    private static final String[] ALL_EFFECT_IDENTIFIERS = EntityUtils.getAllEffectIdentifiers();
    private static final String[] ATTRIBUTES = AttributeType.Builtin.BUILTIN.keySet().toArray(new String[0]);
    private static final String[] ENUM_BOOLEAN = {"true", "false"};
    private static final String[] VALID_COLORS;
    private static final String[] VALID_SCOREBOARD_SLOTS;

    private static final Hash.Strategy<BedrockCommandInfo> PARAM_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(BedrockCommandInfo o) {
            int paramHash = Arrays.deepHashCode(o.paramData());
            return 31 * paramHash + o.description().hashCode();
        }

        @Override
        public boolean equals(BedrockCommandInfo a, BedrockCommandInfo b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if (!a.description().equals(b.description())) return false;
            if (a.paramData().length != b.paramData().length) return false;
            for (int i = 0; i < a.paramData().length; i++) {
                CommandParamData[] a1 = a.paramData()[i];
                CommandParamData[] b1 = b.paramData()[i];
                if (a1.length != b1.length) return false;

                for (int j = 0; j < a1.length; j++) {
                    if (!a1[j].equals(b1[j])) return false;
                }
            }
            return true;
        }
    };

    static {
        List<String> validColors = new ArrayList<>(NamedTextColor.NAMES.keys());
        validColors.add("reset");
        VALID_COLORS = validColors.toArray(new String[0]);

        List<String> teamOptions = new ArrayList<>(Arrays.asList("list", "sidebar", "belowName"));
        for (String color : NamedTextColor.NAMES.keys()) {
            teamOptions.add("sidebar.team." + color);
        }
        VALID_SCOREBOARD_SLOTS = teamOptions.toArray(new String[0]);
    }

    @Override
    public void translate(GeyserSession session, ClientboundCommandsPacket packet) {
        // Don't send command suggestions if they are disabled
        if (!session.getGeyser().getConfig().isCommandSuggestions()) {
            session.getGeyser().getLogger().debug("Not sending translated command suggestions as they are disabled.");

            // Send an empty packet so Bedrock doesn't override /help with its own, built-in help command.
            AvailableCommandsPacket emptyPacket = new AvailableCommandsPacket();
            session.sendUpstreamPacket(emptyPacket);
            return;
        }

        GeyserCommandManager manager = session.getGeyser().commandManager();
        CommandNode[] nodes = packet.getNodes();
        List<CommandData> commandData = new ArrayList<>();
        IntSet commandNodes = new IntOpenHashSet();
        Set<String> knownAliases = new HashSet<>();
        Map<BedrockCommandInfo, Set<String>> commands = new Object2ObjectOpenCustomHashMap<>(PARAM_STRATEGY);
        Int2ObjectMap<List<CommandNode>> commandArgs = new Int2ObjectOpenHashMap<>();

        // Get the first node, it should be a root node
        CommandNode rootNode = nodes[packet.getFirstNodeIndex()];

        // Loop through the root nodes to get all commands
        for (int nodeIndex : rootNode.getChildIndices()) {
            CommandNode node = nodes[nodeIndex];

            // Make sure we don't have duplicated commands (happens if there is more than 1 root node)
            if (!commandNodes.add(nodeIndex) || !knownAliases.add(node.getName().toLowerCase(Locale.ROOT))) continue;

            // Get and update the commandArgs list with the found arguments
            if (node.getChildIndices().length >= 1) {
                for (int childIndex : node.getChildIndices()) {
                    commandArgs.computeIfAbsent(nodeIndex, ($) -> new ArrayList<>()).add(nodes[childIndex]);
                }
            }

            // Get and parse all params
            CommandParamData[][] params = getParams(session, nodes[nodeIndex], nodes);

            // Insert the alias name into the command list
            commands.computeIfAbsent(new BedrockCommandInfo(node.getName().toLowerCase(Locale.ROOT), manager.description(node.getName().toLowerCase(Locale.ROOT)), params),
                    index -> new HashSet<>()).add(node.getName().toLowerCase());
        }

        ServerDefineCommandsEvent event = new ServerDefineCommandsEvent(session, commands.keySet());
        session.getGeyser().eventBus().fire(event);
        if (event.isCancelled()) {
            return;
        }

        // The command flags, not sure what these do apart from break things
        List<CommandData.Flag> flags = Collections.emptyList();

        // Loop through all the found commands
        for (Map.Entry<BedrockCommandInfo, Set<String>> entry : commands.entrySet()) {
            String commandName = entry.getValue().iterator().next(); // We know this has a value

            // Create a basic alias
            CommandEnumData aliases = new CommandEnumData(commandName + "Aliases", entry.getValue().toArray(new String[0]), false);

            // Build the completed command and add it to the final list
            CommandData data = new CommandData(commandName, entry.getKey().description(), flags, (byte) 0, aliases, entry.getKey().paramData());
            commandData.add(data);
        }

        // Add our commands to the AvailableCommandsPacket for the bedrock client
        AvailableCommandsPacket availableCommandsPacket = new AvailableCommandsPacket();
        availableCommandsPacket.getCommands().addAll(commandData);

        session.getGeyser().getLogger().debug("Sending command packet of " + commandData.size() + " commands");

        // Finally, send the commands to the client
        session.sendUpstreamPacket(availableCommandsPacket);
    }

    /**
     * Build the command parameter array for the given command
     *
     * @param session the session
     * @param commandNode The command to build the parameters for
     * @param allNodes    Every command node
     * @return An array of parameter option arrays
     */
    private static CommandParamData[][] getParams(GeyserSession session, CommandNode commandNode, CommandNode[] allNodes) {
        // Check if the command is an alias and redirect it
        if (commandNode.getRedirectIndex() != -1) {
            GeyserImpl.getInstance().getLogger().debug("Redirecting command " + commandNode.getName() + " to " + allNodes[commandNode.getRedirectIndex()].getName());
            commandNode = allNodes[commandNode.getRedirectIndex()];
        }

        if (commandNode.getChildIndices().length >= 1) {
            // Create the root param node and build all the children
            ParamInfo rootParam = new ParamInfo(commandNode, null);
            rootParam.buildChildren(new CommandBuilderContext(session), allNodes);

            List<CommandParamData[]> treeData = rootParam.getTree();

            return treeData.toArray(new CommandParamData[0][]);
        }

        return new CommandParamData[0][0];
    }

    /**
     * Convert Java edition command types to Bedrock edition
     *
     * @param context the session's command context
     * @param node Command type to convert
     * @return Bedrock parameter data type
     */
    private static Object mapCommandType(CommandBuilderContext context, CommandNode node) {
        CommandParser parser = node.getParser();
        if (parser == null) {
            return CommandParam.STRING;
        }

        return switch (parser) {
            case FLOAT, ROTATION, DOUBLE -> CommandParam.FLOAT;
            case INTEGER, LONG -> CommandParam.INT;
            case ENTITY, GAME_PROFILE -> CommandParam.TARGET;
            case BLOCK_POS -> CommandParam.BLOCK_POSITION;
            case COLUMN_POS, VEC3 -> CommandParam.POSITION;
            case MESSAGE -> CommandParam.MESSAGE;
            case NBT_COMPOUND_TAG, NBT_TAG, NBT_PATH -> CommandParam.JSON; //TODO NBT was removed
            case RESOURCE_LOCATION, FUNCTION -> CommandParam.FILE_PATH;
            case BOOL -> ENUM_BOOLEAN;
            case OPERATION -> CommandParam.OPERATOR; // ">=", "==", etc
            case BLOCK_STATE -> context.getBlockStates();
            case ITEM_STACK -> context.session.getItemMappings().getItemNames();
            case COLOR -> VALID_COLORS;
            case SCOREBOARD_SLOT -> VALID_SCOREBOARD_SLOTS;
            case RESOURCE -> handleResource(context, ((ResourceProperties) node.getProperties()).getRegistryKey(), false);
            case RESOURCE_OR_TAG -> handleResource(context, ((ResourceProperties) node.getProperties()).getRegistryKey(), true);
            case DIMENSION -> context.session.getLevels();
            default -> CommandParam.STRING;
        };
    }

    private static Object handleResource(CommandBuilderContext context, String resource, boolean tags) {
        return switch (resource) {
            case "minecraft:attribute" -> ATTRIBUTES;
            case "minecraft:enchantment" -> Enchantment.JavaEnchantment.ALL_JAVA_IDENTIFIERS;
            case "minecraft:entity_type" -> context.getEntityTypes();
            case "minecraft:mob_effect" -> ALL_EFFECT_IDENTIFIERS;
            case "minecraft:worldgen/biome" -> tags ? context.getBiomesWithTags() : context.getBiomes();
            default -> CommandParam.STRING;
        };
    }

    /**
     * Stores the command description and parameter data for best optimizing the Bedrock commands packet.
     */
    private record BedrockCommandInfo(String name, String description, CommandParamData[][] paramData) implements ServerDefineCommandsEvent.CommandInfo {
    }

    /**
     * Stores command completions so we don't have to rebuild the same values multiple times.
     */
    @MonotonicNonNull
    private static class CommandBuilderContext {
        private final GeyserSession session;
        private Object biomesWithTags;
        private Object biomesNoTags;
        private String[] blockStates;
        private String[] entityTypes;

        CommandBuilderContext(GeyserSession session) {
            this.session = session;
        }

        private Object getBiomes() {
            if (biomesNoTags != null) {
                return biomesNoTags;
            }

            String[] identifiers = session.getGeyser().getWorldManager().getBiomeIdentifiers(false);
            return (biomesNoTags = identifiers != null ? identifiers : CommandParam.STRING);
        }

        private Object getBiomesWithTags() {
            if (biomesWithTags != null) {
                return biomesWithTags;
            }

            String[] identifiers = session.getGeyser().getWorldManager().getBiomeIdentifiers(true);
            return (biomesWithTags = identifiers != null ? identifiers : CommandParam.STRING);
        }

        private String[] getBlockStates() {
            if (blockStates != null) {
                return blockStates;
            }
            return (blockStates = BlockRegistries.JAVA_TO_BEDROCK_IDENTIFIERS.get().keySet().toArray(new String[0]));
        }

        private String[] getEntityTypes() {
            if (entityTypes != null) {
                return entityTypes;
            }
            return (entityTypes = Registries.JAVA_ENTITY_IDENTIFIERS.get().keySet().toArray(new String[0]));
        }
    }

    @Getter
    @ToString
    private static class ParamInfo {
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
         * Build the array of all the child parameters (recursive)
         *
         * @param context the session's command builder context
         * @param allNodes Every command node
         */
        public void buildChildren(CommandBuilderContext context, CommandNode[] allNodes) {
            for (int paramID : paramNode.getChildIndices()) {
                CommandNode paramNode = allNodes[paramID];

                if (paramNode == this.paramNode) {
                    // Fixes a StackOverflowError when an argument has itself as a child
                    continue;
                }

                if (paramNode.getParser() == null) {
                    boolean foundCompatible = false;
                    for (int i = 0; i < children.size(); i++) {
                        ParamInfo enumParamInfo = children.get(i);
                        // Check to make sure all descending nodes of this command are compatible - otherwise, create a new overload
                        if (isCompatible(allNodes, enumParamInfo.getParamNode(), paramNode)) {
                            foundCompatible = true;
                            // Extend the current list of enum values
                            String[] enumOptions = Arrays.copyOf(enumParamInfo.getParamData().getEnumData().getValues(), enumParamInfo.getParamData().getEnumData().getValues().length + 1);
                            enumOptions[enumOptions.length - 1] = paramNode.getName();

                            // Re-create the command using the updated values
                            CommandEnumData enumData = new CommandEnumData(enumParamInfo.getParamData().getEnumData().getName(), enumOptions, false);
                            children.set(i, new ParamInfo(enumParamInfo.getParamNode(), new CommandParamData(enumParamInfo.getParamData().getName(), this.paramNode.isExecutable(), enumData, null, null, Collections.emptyList())));
                            break;
                        }
                    }

                    if (!foundCompatible) {
                        // Create a new subcommand with this exact type
                        CommandEnumData enumData = new CommandEnumData(paramNode.getName(), new String[]{paramNode.getName()}, false);

                        // On setting optional:
                        // isExecutable is defined as a node "constitutes a valid command."
                        // Therefore, any children of the parameter must simply be optional.
                        children.add(new ParamInfo(paramNode, new CommandParamData(paramNode.getName(), this.paramNode.isExecutable(), enumData, null, null, Collections.emptyList())));
                    }
                } else {
                    // Put the non-enum param into the list
                    Object mappedType = mapCommandType(context, paramNode);
                    CommandEnumData enumData = null;
                    CommandParam type = null;
                    boolean optional = this.paramNode.isExecutable();
                    if (mappedType instanceof String[]) {
                        enumData = new CommandEnumData(getEnumDataName(paramNode).toLowerCase(Locale.ROOT), (String[]) mappedType, false);
                    } else {
                        type = (CommandParam) mappedType;
                        // Bedrock throws a fit if an optional message comes after a string or target
                        // Example vanilla commands: ban-ip, ban, and kick
                        if (optional && type == CommandParam.MESSAGE && (paramData.getType() == CommandParam.STRING || paramData.getType() == CommandParam.TARGET)) {
                            optional = false;
                        }
                    }
                    // IF enumData != null:
                    // In game, this will show up like <paramNode.getName(): enumData.getName()>
                    // So if paramNode.getName() == "value" and enumData.getName() == "bool": <value: bool>
                    children.add(new ParamInfo(paramNode, new CommandParamData(paramNode.getName(), optional, enumData, type, null, Collections.emptyList())));
                }
            }

            // Recursively build all child options
            for (ParamInfo child : children) {
                child.buildChildren(context, allNodes);
            }
        }

        /**
         * Mitigates https://github.com/GeyserMC/Geyser/issues/3411. Not a perfect solution.
         */
        private static String getEnumDataName(CommandNode node) {
            if (node.getProperties() instanceof ResourceProperties properties) {
                String registryKey = properties.getRegistryKey();
                int identifierSplit = registryKey.indexOf(':');
                if (identifierSplit != -1) {
                    return registryKey.substring(identifierSplit);
                }
                return registryKey;
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
         *
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
        public List<CommandParamData[]> getTree() {
            List<CommandParamData[]> treeParamData = new ArrayList<>();

            for (ParamInfo child : children) {
                // Get the tree from the child
                List<CommandParamData[]> childTree = child.getTree();

                // Un-pack the tree append the child node to it and push into the list
                for (CommandParamData[] subChild : childTree) {
                    CommandParamData[] tmpTree = new CommandParamData[subChild.length + 1];
                    tmpTree[0] = child.getParamData();
                    System.arraycopy(subChild, 0, tmpTree, 1, subChild.length);

                    treeParamData.add(tmpTree);
                }

                // If we have no more child parameters just the child
                if (childTree.size() == 0) {
                    treeParamData.add(new CommandParamData[] { child.getParamData() });
                }
            }

            return treeParamData;
        }
    }
}
