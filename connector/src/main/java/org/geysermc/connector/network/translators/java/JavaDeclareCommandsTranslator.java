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

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.protocol.data.game.command.CommandNode;
import com.github.steveice10.mc.protocol.data.game.command.CommandParser;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareCommandsPacket;
import com.nukkitx.protocol.bedrock.data.command.CommandData;
import com.nukkitx.protocol.bedrock.data.command.CommandEnumData;
import com.nukkitx.protocol.bedrock.data.command.CommandParamData;
import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import com.nukkitx.protocol.bedrock.packet.AvailableCommandsPacket;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import java.util.*;

@Translator(packet = ServerDeclareCommandsPacket.class)
public class JavaDeclareCommandsTranslator extends PacketTranslator<ServerDeclareCommandsPacket> {

    private static final String[] ENUM_BOOLEAN = {"true", "false"};

    private static final Hash.Strategy<CommandParamData[][]> PARAM_STRATEGY = new Hash.Strategy<CommandParamData[][]>() {
        @Override
        public int hashCode(CommandParamData[][] o) {
            return Arrays.deepHashCode(o);
        }

        @Override
        public boolean equals(CommandParamData[][] a, CommandParamData[][] b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if (a.length != b.length) return false;
            for (int i = 0; i < a.length; i++) {
                CommandParamData[] a1 = a[i];
                CommandParamData[] b1 = b[i];
                if (a1.length != b1.length) return false;

                for (int i2 = 0; i2 < a1.length; i2++) {
                    if (!a1[i].equals(b1[i])) return false;
                }
            }
            return true;
        }
    };

    /**
     * Build the command parameter array for the given command
     *
     * @param commandNode The command to build the parameters for
     * @param allNodes    Every command node
     * @return An array of parameter option arrays
     */
    private static CommandParamData[][] getParams(CommandNode commandNode, CommandNode[] allNodes) {
        // Check if the command is an alias and redirect it
        if (commandNode.getRedirectIndex() != -1) {
            GeyserConnector.getInstance().getLogger().debug("Redirecting command " + commandNode.getName() + " to " + allNodes[commandNode.getRedirectIndex()].getName());
            commandNode = allNodes[commandNode.getRedirectIndex()];
        }

        if (commandNode.getChildIndices().length >= 1) {
            // Create the root param node and build all the children
            ParamInfo rootParam = new ParamInfo(commandNode, null);
            rootParam.buildChildren(allNodes);

            List<CommandParamData[]> treeData = rootParam.getTree();

            return treeData.toArray(new CommandParamData[0][]);
        }

        return new CommandParamData[0][0];
    }

    /**
     * Convert Java edition command types to Bedrock edition
     *
     * @param parser Command type to convert
     * @return Bedrock parameter data type
     */
    private static Object mapCommandType(CommandParser parser) {
        if (parser == null) {
            return CommandParamType.STRING;
        }

        switch (parser) {
            case FLOAT:
            case ROTATION:
            case DOUBLE:
                return CommandParamType.FLOAT;

            case INTEGER:
                return CommandParamType.INT;

            case ENTITY:
            case GAME_PROFILE:
                return CommandParamType.TARGET;

            case BLOCK_POS:
                return CommandParamType.BLOCK_POSITION;

            case COLUMN_POS:
            case VEC3:
                return CommandParamType.POSITION;

            case MESSAGE:
                return CommandParamType.MESSAGE;

            case NBT:
            case NBT_COMPOUND_TAG:
            case NBT_TAG:
            case NBT_PATH:
                return CommandParamType.JSON;

            case RESOURCE_LOCATION:
            case FUNCTION:
                return CommandParamType.FILE_PATH;

            case INT_RANGE:
                return CommandParamType.INT_RANGE;

            case BOOL:
                return ENUM_BOOLEAN;

            case OPERATION: // Possibly OPERATOR
                return CommandParamType.OPERATOR;

            case STRING:
            case VEC2:
            case BLOCK_STATE:
            case BLOCK_PREDICATE:
            case ITEM_STACK:
            case ITEM_PREDICATE:
            case COLOR:
            case COMPONENT:
            case OBJECTIVE:
            case OBJECTIVE_CRITERIA:
            case PARTICLE:
            case SCOREBOARD_SLOT:
            case SCORE_HOLDER:
            case SWIZZLE:
            case TEAM:
            case ITEM_SLOT:
            case MOB_EFFECT:
            case ENTITY_ANCHOR:
            case RANGE:
            case FLOAT_RANGE:
            case ITEM_ENCHANTMENT:
            case ENTITY_SUMMON:
            case DIMENSION:
            case TIME:
            default:
                return CommandParamType.STRING;
        }
    }

    @Override
    public void translate(ServerDeclareCommandsPacket packet, GeyserSession session) {
        // Don't send command suggestions if they are disabled
        if (!session.getConnector().getConfig().isCommandSuggestions()) {
            session.getConnector().getLogger().debug("Not sending command suggestions as they are disabled.");
            return;
        }
        CommandNode[] nodes = packet.getNodes();
        List<CommandData> commandData = new ArrayList<>();
        IntSet commandNodes = new IntOpenHashSet();
        Set<String> knownAliases = new HashSet<>();
        Map<CommandParamData[][], Set<String>> commands = new Object2ObjectOpenCustomHashMap<>(PARAM_STRATEGY);
        Int2ObjectMap<List<CommandNode>> commandArgs = new Int2ObjectOpenHashMap<>();

        // Get the first node, it should be a root node
        CommandNode rootNode = nodes[packet.getFirstNodeIndex()];

        // Loop through the root nodes to get all commands
        for (int nodeIndex : rootNode.getChildIndices()) {
            CommandNode node = nodes[nodeIndex];

            // Make sure we don't have duplicated commands (happens if there is more than 1 root node)
            if (!commandNodes.add(nodeIndex) || !knownAliases.add(node.getName().toLowerCase())) continue;

            // Get and update the commandArgs list with the found arguments
            if (node.getChildIndices().length >= 1) {
                for (int childIndex : node.getChildIndices()) {
                    commandArgs.putIfAbsent(nodeIndex, new ArrayList<>());
                    commandArgs.get(nodeIndex).add(nodes[childIndex]);
                }
            }

            // Get and parse all params
            CommandParamData[][] params = getParams(nodes[nodeIndex], nodes);

            // Insert the alias name into the command list
            commands.computeIfAbsent(params, index -> new HashSet<>()).add(node.getName().toLowerCase());
        }

        // The command flags, not sure what these do apart from break things
        List<CommandData.Flag> flags = new ArrayList<>();

        // Loop through all the found commands

        for (Map.Entry<CommandParamData[][], Set<String>> entry : commands.entrySet()) {
            String commandName = entry.getValue().iterator().next(); // We know this has a value

            // Create a basic alias
            CommandEnumData aliases = new CommandEnumData(commandName + "Aliases", entry.getValue().toArray(new String[0]), false);

            // Build the completed command and add it to the final list
            CommandData data = new CommandData(commandName, session.getConnector().getCommandManager().getDescription(commandName), flags, (byte) 0, aliases, entry.getKey());
            commandData.add(data);
        }

        // Add our commands to the AvailableCommandsPacket for the bedrock client
        AvailableCommandsPacket availableCommandsPacket = new AvailableCommandsPacket();
        for (CommandData data : commandData) {
            availableCommandsPacket.getCommands().add(data);
        }

        GeyserConnector.getInstance().getLogger().debug("Sending command packet of " + commandData.size() + " commands");

        // Finally, send the commands to the client
        session.sendUpstreamPacket(availableCommandsPacket);
    }

    @Getter
    private static class ParamInfo {
        private CommandNode paramNode;
        private CommandParamData paramData;
        private List<ParamInfo> children;

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
         * @param allNodes Every command node
         */
        public void buildChildren(CommandNode[] allNodes) {
            int enumIndex = -1;

            for (int paramID : paramNode.getChildIndices()) {
                CommandNode paramNode = allNodes[paramID];

                if (paramNode.getParser() == null) {
                    if (enumIndex == -1) {
                        enumIndex = children.size();

                        // Create the new enum command
                        Object mappedType = mapCommandType(paramNode.getParser());
                        CommandEnumData enumData;
                        CommandParamType type = null;
                        if (mappedType instanceof String[]) {
                            enumData = new CommandEnumData(paramNode.getName(), (String[]) mappedType, false);
                        } else {
                            enumData = new CommandEnumData(paramNode.getName(), new String[]{paramNode.getName()}, false);
                            type = (CommandParamType) mappedType;
                        }

                        children.add(new ParamInfo(paramNode, new CommandParamData(paramNode.getName(), false, enumData, type, null, Collections.emptyList())));
                    } else {
                        // Get the existing enum
                        ParamInfo enumParamInfo = children.get(enumIndex);

                        // Extend the current list of enum values
                        String[] enumOptions = Arrays.copyOf(enumParamInfo.getParamData().getEnumData().getValues(), enumParamInfo.getParamData().getEnumData().getValues().length + 1);
                        enumOptions[enumOptions.length - 1] = paramNode.getName();

                        // Re-create the command using the updated values
                        CommandEnumData enumData = new CommandEnumData(enumParamInfo.getParamData().getEnumData().getName(), enumOptions, false);
                        children.set(enumIndex, new ParamInfo(enumParamInfo.getParamNode(), new CommandParamData(enumParamInfo.getParamData().getName(), false, enumData, enumParamInfo.getParamData().getType(), null, Collections.emptyList())));
                    }
                } else {
                    // Put the non-enum param into the list
                    Object mappedType = mapCommandType(paramNode.getParser());
                    CommandEnumData enumData = null;
                    CommandParamType type = null;
                    if (mappedType instanceof String[]) {
                        enumData = new CommandEnumData(paramNode.getName(), new String[]{paramNode.getName()}, false);
                    } else {
                        type = (CommandParamType) mappedType;
                    }
                    children.add(new ParamInfo(paramNode, new CommandParamData(paramNode.getName(), false, enumData, type, null, Collections.emptyList())));
                }
            }

            // Recursively build all child options
            for (ParamInfo child : children) {
                child.buildChildren(allNodes);
            }
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
                for (CommandParamData[] subchild : childTree) {
                    CommandParamData[] tmpTree = new ArrayList<CommandParamData>() {
                        {
                            add(child.getParamData());
                            addAll(Arrays.asList(subchild));
                        }
                    }.toArray(new CommandParamData[0]);

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
