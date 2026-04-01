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

import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.cloudburstmc.protocol.bedrock.data.command.*;
import org.cloudburstmc.protocol.bedrock.packet.AvailableCommandsPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.java.ServerDefineCommandsEvent;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandParser;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.ResourceProperties;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;

import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("removal") 
@Translator(packet = ClientboundCommandsPacket.class)
public class JavaCommandsTranslator extends PacketTranslator<ClientboundCommandsPacket> {

    
    private static final Supplier<String[]> ALL_BLOCK_NAMES = Suppliers.memoize(() -> BlockRegistries.JAVA_BLOCKS.get().stream().map(block -> block.javaIdentifier().toString()).toArray(String[]::new));
    private static final String[] ALL_EFFECT_IDENTIFIERS = EntityUtils.getAllEffectIdentifiers();
    private static final String[] ATTRIBUTES = AttributeType.Builtin.BUILTIN.values().stream().map(type -> type.getIdentifier().asString()).toList().toArray(new String[0]);
    private static final String[] ENUM_BOOLEAN = {"true", "false"};
    private static final String[] VALID_COLORS;
    private static final String[] VALID_SCOREBOARD_SLOTS;

    private static final Hash.Strategy<BedrockCommandInfo> PARAM_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(BedrockCommandInfo o) {
            int paramHash = Arrays.deepHashCode(o.paramData());
            if ("help".equals(o.name())) {
                paramHash = 31 * paramHash + 1;
            }
            return 31 * paramHash + o.description().hashCode();
        }

        @Override
        public boolean equals(BedrockCommandInfo a, BedrockCommandInfo b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if ("help".equals(a.name()) && !"help".equals(b.name())) {
                
                
                
                return false;
            }
            if (!a.description().equals(b.description())) return false;
            if (a.paramData().length != b.paramData().length) return false;
            for (int i = 0; i < a.paramData().length; i++) {
                CommandParamData[] a1 = a.paramData()[i].getOverloads();
                CommandParamData[] b1 = b.paramData()[i].getOverloads();
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
        
        if (!session.getGeyser().config().gameplay().commandSuggestions()) {
            session.getGeyser().getLogger().debug("Not sending translated command suggestions as they are disabled.");

            
            AvailableCommandsPacket emptyPacket = new AvailableCommandsPacket();
            emptyPacket.getCommands().add(createFakeHelpCommand());
            session.sendUpstreamPacket(emptyPacket);
            return;
        }

        CommandRegistry registry = session.getGeyser().commandRegistry();
        CommandNode[] nodes = packet.getNodes();
        List<CommandData> commandData = new ArrayList<>();
        IntSet commandNodes = new IntOpenHashSet();
        Set<String> knownAliases = new HashSet<>();
        Map<BedrockCommandInfo, Set<String>> commands = new Object2ObjectOpenCustomHashMap<>(PARAM_STRATEGY);
        Int2ObjectMap<List<CommandNode>> commandArgs = new Int2ObjectOpenHashMap<>();

        
        CommandNode rootNode = nodes[packet.getFirstNodeIndex()];

        List<String> knownCommands = new ArrayList<>();
        List<String> restrictedCommands = new ArrayList<>();
        
        for (int nodeIndex : rootNode.getChildIndices()) {
            CommandNode node = nodes[nodeIndex];

            
            if (!commandNodes.add(nodeIndex) || !knownAliases.add(node.getName().toLowerCase(Locale.ROOT))) continue;

            
            if (node.getChildIndices().length >= 1) {
                for (int childIndex : node.getChildIndices()) {
                    commandArgs.computeIfAbsent(nodeIndex, ($) -> new ArrayList<>()).add(nodes[childIndex]);
                }
            }

            
            CommandOverloadData[] params = getParams(session, nodes[nodeIndex], nodes);

            
            String name = node.getName().toLowerCase(Locale.ROOT);
            String description = registry.description(name, session.locale());
            BedrockCommandInfo info = new BedrockCommandInfo(name, description, params);
            commands.computeIfAbsent(info, $ -> new HashSet<>()).add(name);

            
            knownCommands.add(name);
            if (node.isAllowsRestricted()) { 
                restrictedCommands.add(name);
            }
        }

        session.setKnownCommands(knownCommands);
        session.setRestrictedCommands(restrictedCommands);

        var eventBus = session.getGeyser().eventBus();

        var event = new ServerDefineCommandsEvent(session, commands.keySet());
        eventBus.fire(event);
        if (event.isCancelled()) {
            return;
        }

        var oldEvent = new org.geysermc.geyser.api.event.downstream.ServerDefineCommandsEvent(session, commands.keySet());
        eventBus.fire(oldEvent);
        if (oldEvent.isCancelled()) {
            return;
        }

        
        Set<CommandData.Flag> flags = Set.of(CommandData.Flag.NOT_CHEAT);

        boolean helpAdded = false;

        
        for (Map.Entry<BedrockCommandInfo, Set<String>> entry : commands.entrySet()) {
            String commandName = entry.getValue().iterator().next(); 

            LinkedHashMap<String, Set<CommandEnumConstraint>> values = new LinkedHashMap<>();
            
            for (String s : entry.getValue()) {
                values.put(s, EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES));
            }

            
            CommandEnumData aliases = new CommandEnumData(commandName + "Aliases", values, false);

            
            String description = entry.getKey().description();

            
            int lineBreak = description.indexOf('\n');
            if (lineBreak >= 0) {
                description = description.substring(0, lineBreak);
            }

            
            
            
            if (description.length() > 950) {
                description = description.substring(0, 947) + "...";
            }

            
            CommandData data = new CommandData(commandName, description, flags, CommandPermission.ANY, aliases, Collections.emptyList(), entry.getKey().paramData());
            commandData.add(data);

            if (commandName.equals("help")) {
                helpAdded = true;
            }
        }

        if (!helpAdded) {
            
            commandData.add(createFakeHelpCommand());
        }

        if (session.getGeyser().platformType() == PlatformType.STANDALONE) {
            session.getGeyser().commandRegistry().export(session, commandData, knownAliases);
        }

        
        AvailableCommandsPacket availableCommandsPacket = new AvailableCommandsPacket();
        availableCommandsPacket.getCommands().addAll(commandData);

        session.getGeyser().getLogger().debug("Sending command packet of " + commandData.size() + " commands");

        
        session.sendUpstreamPacket(availableCommandsPacket);
    }

    
    private static CommandOverloadData[] getParams(GeyserSession session, CommandNode commandNode, CommandNode[] allNodes) {
        
        if (commandNode.getRedirectIndex().isPresent()) {
            int redirectIndex = commandNode.getRedirectIndex().getAsInt();
            GeyserImpl.getInstance().getLogger().debug("Redirecting command " + commandNode.getName() + " to " + allNodes[redirectIndex].getName());
            commandNode = allNodes[redirectIndex];
        }

        if (commandNode.getChildIndices().length >= 1) {
            
            ParamInfo rootParam = new ParamInfo(commandNode, null);
            rootParam.buildChildren(new CommandBuilderContext(session), allNodes);

            List<CommandOverloadData> treeData = rootParam.getTree();

            return treeData.toArray(new CommandOverloadData[0]);
        }

        return new CommandOverloadData[0];
    }

    
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
            case NBT_COMPOUND_TAG, NBT_TAG, NBT_PATH -> CommandParam.JSON; 
            case RESOURCE_LOCATION, FUNCTION -> CommandParam.FILE_PATH;
            case BOOL -> ENUM_BOOLEAN;
            case OPERATION -> CommandParam.OPERATOR; 
            case BLOCK_STATE -> ALL_BLOCK_NAMES.get();
            case ITEM_STACK -> context.getItemNames();
            case COLOR -> VALID_COLORS;
            case SCOREBOARD_SLOT -> VALID_SCOREBOARD_SLOTS;
            case RESOURCE -> handleResource(context, ((ResourceProperties) node.getProperties()).getRegistryKey(), false);
            case RESOURCE_OR_TAG -> handleResource(context, ((ResourceProperties) node.getProperties()).getRegistryKey(), true);
            case DIMENSION -> context.session.getLevels();
            case TEAM -> context.getTeams(); 
            default -> CommandParam.STRING;
        };
    }

    private static Object handleResource(CommandBuilderContext context, Key resource, boolean tags) {
        return switch (resource.asString()) {
            case "minecraft:attribute" -> ATTRIBUTES;
            case "minecraft:enchantment" -> context.getEnchantments();
            case "minecraft:entity_type" -> context.getEntityTypes();
            case "minecraft:mob_effect" -> ALL_EFFECT_IDENTIFIERS;
            case "minecraft:worldgen/biome" -> tags ? context.getBiomesWithTags() : context.getBiomes();
            default -> CommandParam.STRING;
        };
    }

    private CommandData createFakeHelpCommand() {
        CommandEnumData aliases = new CommandEnumData("helpAliases", Map.of("help", EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES)), false);
        return new CommandData("help", "", Set.of(CommandData.Flag.NOT_CHEAT), CommandPermission.ANY, aliases, Collections.emptyList(), new CommandOverloadData[0]);
    }

    
    private record BedrockCommandInfo(String name, String description, CommandOverloadData[] paramData) implements
            org.geysermc.geyser.api.event.downstream.ServerDefineCommandsEvent.CommandInfo,
            ServerDefineCommandsEvent.CommandInfo
    {
    }

    
    @MonotonicNonNull
    private static class CommandBuilderContext {
        private final GeyserSession session;
        private Object biomesWithTags;
        private Object biomesNoTags;
        private String[] enchantments;
        private String[] entityTypes;
        private String[] itemNames;
        private CommandEnumData teams;

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

        private String[] getEnchantments() {
            if (enchantments != null) {
                return enchantments;
            }
            return (enchantments = session.getRegistryCache().registry(JavaRegistries.ENCHANTMENT).keys().stream().map(Key::asString).toArray(String[]::new));
        }

        private String[] getEntityTypes() {
            if (entityTypes != null) {
                return entityTypes;
            }
            return (entityTypes = Registries.JAVA_ENTITY_IDENTIFIERS.get().keySet().toArray(new String[0]));
        }

        public String[] getItemNames() {
            if (itemNames != null) {
                return itemNames;
            }
            return (itemNames = Registries.JAVA_ITEM_IDENTIFIERS.get().keySet().toArray(new String[0]));
        }

        private CommandEnumData getTeams() {
            if (teams != null) {
                return teams;
            }
            return (teams = new CommandEnumData("Geyser_Teams",
                    session.getWorldCache().getScoreboard().getTeamNames(), true
            ));
        }
    }

    @Getter
    @ToString
    private static class ParamInfo {
        private final CommandNode paramNode;
        private final CommandParamData paramData;
        private final List<ParamInfo> children;

        
        public ParamInfo(CommandNode paramNode, CommandParamData paramData) {
            this.paramNode = paramNode;
            this.paramData = paramData;
            this.children = new ArrayList<>();
        }

        
        public void buildChildren(CommandBuilderContext context, CommandNode[] allNodes) {
            for (int paramID : paramNode.getChildIndices()) {
                CommandNode paramNode = allNodes[paramID];

                if (paramNode == this.paramNode) {
                    
                    continue;
                }

                if (paramNode.getParser() == null) {
                    boolean foundCompatible = false;
                    for (int i = 0; i < children.size(); i++) {
                        ParamInfo enumParamInfo = children.get(i);
                        
                        if (isCompatible(allNodes, enumParamInfo.getParamNode(), paramNode)) {
                            foundCompatible = true;
                            
                            
                            
                            

                            Map<String, Set<CommandEnumConstraint>> values = new LinkedHashMap<>(enumParamInfo.getParamData().getEnumData().getValues());
                            values.put(paramNode.getName(), Set.of());

                            
                            CommandEnumData enumData = new CommandEnumData(enumParamInfo.getParamData().getEnumData().getName(), values, false);
                            CommandParamData commandParamData = new CommandParamData();
                            commandParamData.setName(enumParamInfo.getParamData().getName());
                            commandParamData.setOptional(this.paramNode.isExecutable());
                            commandParamData.setEnumData(enumData);

                            children.set(i, new ParamInfo(enumParamInfo.getParamNode(), commandParamData));
                            break;
                        }
                    }

                    if (!foundCompatible) {
                        
                        LinkedHashMap<String, Set<CommandEnumConstraint>> map = new LinkedHashMap<>();
                        map.put(paramNode.getName(), Set.of());
                        CommandEnumData enumData = new CommandEnumData(paramNode.getName(), map, false);

                        
                        
                        
                        CommandParamData commandParamData = new CommandParamData();
                        commandParamData.setName(paramNode.getName());
                        commandParamData.setOptional(this.paramNode.isExecutable());
                        commandParamData.setEnumData(enumData);

                        children.add(new ParamInfo(paramNode, commandParamData));
                    }
                } else {
                    
                    Object mappedType = mapCommandType(context, paramNode);
                    CommandEnumData enumData = null;
                    CommandParam type = null;
                    boolean optional = this.paramNode.isExecutable();
                    if (mappedType instanceof CommandEnumData) {
                        
                        enumData = (CommandEnumData) mappedType;
                    } else if (mappedType instanceof String[]) {
                        LinkedHashMap<String, Set<CommandEnumConstraint>> map = new LinkedHashMap<>();
                        for (String s : (String[]) mappedType) {
                            map.put(s, Set.of());
                        }

                        enumData = new CommandEnumData(getEnumDataName(paramNode).toLowerCase(Locale.ROOT), map, false);
                    } else {
                        type = (CommandParam) mappedType;
                        
                        
                        if (optional && type == CommandParam.MESSAGE && paramData != null && (paramData.getType() == CommandParam.STRING || paramData.getType() == CommandParam.TARGET)) {
                            optional = false;
                        }
                    }
                    
                    
                    
                    CommandParamData commandParamData = new CommandParamData();
                    commandParamData.setName(paramNode.getName());
                    commandParamData.setOptional(optional);
                    commandParamData.setEnumData(enumData);
                    commandParamData.setType(type);

                    children.add(new ParamInfo(paramNode, commandParamData));
                }
            }

            
            for (ParamInfo child : children) {
                child.buildChildren(context, allNodes);
            }
        }

        
        private static String getEnumDataName(CommandNode node) {
            if (node.getProperties() instanceof ResourceProperties properties) {
                Key registryKey = properties.getRegistryKey();
                return registryKey.value();
            }
            return node.getParser().name();
        }

        
        private boolean isCompatible(CommandNode[] allNodes, CommandNode a, CommandNode b) {
            if (a == b) return true;
            if (a.getParser() != b.getParser()) return false;
            if (a.getChildIndices().length != b.getChildIndices().length) return false;

            for (int i = 0; i < a.getChildIndices().length; i++) {
                boolean hasSimilarity = false;
                CommandNode a1 = allNodes[a.getChildIndices()[i]];
                
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

        
        public List<CommandOverloadData> getTree() {
            List<CommandOverloadData> treeParamData = new ArrayList<>();

            for (ParamInfo child : children) {
                
                List<CommandOverloadData> childTree = child.getTree();

                
                for (CommandOverloadData subChildData : childTree) {
                    CommandParamData[] subChild = subChildData.getOverloads();
                    CommandParamData[] tmpTree = new CommandParamData[subChild.length + 1];
                    tmpTree[0] = child.getParamData();
                    System.arraycopy(subChild, 0, tmpTree, 1, subChild.length);

                    treeParamData.add(new CommandOverloadData(false, tmpTree));
                }

                
                if (childTree.size() == 0) {
                    treeParamData.add(new CommandOverloadData(false, new CommandParamData[] { child.getParamData() }));
                }
            }

            return treeParamData;
        }
    }
}
