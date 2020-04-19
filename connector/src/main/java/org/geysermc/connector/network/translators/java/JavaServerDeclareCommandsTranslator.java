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
import com.nukkitx.protocol.bedrock.data.CommandData;
import com.nukkitx.protocol.bedrock.data.CommandEnumData;
import com.nukkitx.protocol.bedrock.data.CommandParamData;
import com.nukkitx.protocol.bedrock.packet.AvailableCommandsPacket;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import java.util.*;

@Translator(packet = ServerDeclareCommandsPacket.class)
public class JavaServerDeclareCommandsTranslator extends PacketTranslator<ServerDeclareCommandsPacket> {
    @Override
    public void translate(ServerDeclareCommandsPacket packet, GeyserSession session) {
        List<CommandData> commandData = new ArrayList<>();
        Map<Integer, String> commands = new HashMap<>();
        Map<Integer, List<CommandNode>> commandArgs = new HashMap<>();

        // Get the first node, it should be a root node
        CommandNode rootNode = packet.getNodes()[packet.getFirstNodeIndex()];

        // Loop through the root nodes to get all commands
        for (int nodeIndex : rootNode.getChildIndices()) {
            CommandNode node = packet.getNodes()[nodeIndex];

            // Make sure we dont have duplicated commands (happens if there is more than 1 root node)
            if (commands.containsKey(nodeIndex)) { continue; }

            // Get and update the commandArgs list with the found arguments
            if (node.getChildIndices().length >= 1) {
                for (int childIndex : node.getChildIndices()) {
                    commandArgs.putIfAbsent(nodeIndex, new ArrayList<>());
                    commandArgs.get(nodeIndex).add(packet.getNodes()[childIndex]);
                }
            }

            // Insert the command name into the list
            commands.put(nodeIndex, node.getName());
        }

        // The command flags, not sure what these do apart from break things
        List<CommandData.Flag> flags = new ArrayList<>();

        // Loop through all the found commands
        for (int commandID : commands.keySet()) {
            String commandName = commands.get(commandID);

            // Create a basic alias
            CommandEnumData aliases = new CommandEnumData( commandName + "Aliases", new String[] { commandName.toLowerCase() }, false);

            // Get and parse all params
            CommandParamData[][] params = getParams(commandID, packet.getNodes()[commandID], packet.getNodes());

            // Build the completed command and add it to the final list
            CommandData data = new CommandData(commandName, "", flags, (byte) 0, aliases, params);
            commandData.add(data);
        }

        // Add our commands to the AvailableCommandsPacket for the bedrock client
        AvailableCommandsPacket availableCommandsPacket = new AvailableCommandsPacket();
        for (CommandData data : commandData) {
            availableCommandsPacket.getCommands().add(data);
        }

        GeyserConnector.getInstance().getLogger().debug("Sending command packet of " + commandData.size() + " commands");

        // Finally, send the commands to the client
        session.getUpstream().sendPacket(availableCommandsPacket);
    }

    private CommandParamData[][] getParams(int commandID, CommandNode commandNode, CommandNode[] allNodes) {
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
            CommandParamData[][] params = new CommandParamData[treeData.size()][];

            // Fill the nested params array
            int i = 0;
            for (CommandParamData[] tree : treeData) {
                params[i] = tree;
                i++;
            }

            return params;
        }

        return new CommandParamData[0][0];
    }

    private CommandParamData.Type mapCommandType(CommandParser parser) {
        if (parser == null) { return CommandParamData.Type.STRING; }

        switch (parser) {
            case FLOAT:
                return CommandParamData.Type.FLOAT;

            case INTEGER:
                return CommandParamData.Type.INT;

            case ENTITY:
            case GAME_PROFILE:
                return CommandParamData.Type.TARGET;

            case BLOCK_POS:
                return CommandParamData.Type.BLOCK_POSITION;

            case COLUMN_POS:
            case VEC3:
                return CommandParamData.Type.POSITION;

            case MESSAGE:
                return CommandParamData.Type.MESSAGE;

            case NBT:
            case NBT_COMPOUND_TAG:
            case NBT_TAG:
            case NBT_PATH:
                return CommandParamData.Type.JSON;

            case RESOURCE_LOCATION:
                return CommandParamData.Type.FILE_PATH;

            case INT_RANGE:
                return CommandParamData.Type.INT_RANGE;

            case BOOL:
            case DOUBLE:
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
            case OPERATION: // Possibly OPERATOR
            case PARTICLE:
            case ROTATION:
            case SCOREBOARD_SLOT:
            case SCORE_HOLDER:
            case SWIZZLE:
            case TEAM:
            case ITEM_SLOT:
            case MOB_EFFECT:
            case FUNCTION:
            case ENTITY_ANCHOR:
            case RANGE:
            case FLOAT_RANGE:
            case ITEM_ENCHANTMENT:
            case ENTITY_SUMMON:
            case DIMENSION:
            case TIME:
            default:
                return CommandParamData.Type.STRING;
        }
    }

    @Getter
    private class ParamInfo {
        private CommandNode paramNode;
        private CommandParamData paramData;
        private List<ParamInfo> children;

        public ParamInfo(CommandNode paramNode, CommandParamData paramData) {
            this.paramNode = paramNode;
            this.paramData = paramData;
            this.children = new ArrayList<>();
        }

        public void buildChildren(CommandNode[] allNodes) {
            int enumIndex = -1;

            for (int paramID : paramNode.getChildIndices()) {
                CommandNode paramNode = allNodes[paramID];

                if (paramNode.getParser() == null) {
                    if (enumIndex == -1) {
                        enumIndex = children.size();

                        // Create the new enum command
                        CommandEnumData enumData = new CommandEnumData(paramNode.getName(), new String[] { paramNode.getName() }, false);
                        children.add(new ParamInfo(paramNode, new CommandParamData(paramNode.getName(), false, enumData, mapCommandType(paramNode.getParser()), null, Collections.emptyList())));
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
                }else{
                    // Put the non-enum param into the list
                    children.add(new ParamInfo(paramNode, new CommandParamData(paramNode.getName(), false, null, mapCommandType(paramNode.getParser()), null, Collections.emptyList())));
                }
            }

            // Recursively build all child options
            for (ParamInfo child : children) {
                child.buildChildren(allNodes);
            }
        }

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
