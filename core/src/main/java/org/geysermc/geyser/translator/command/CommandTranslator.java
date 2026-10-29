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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.cloudburstmc.protocol.bedrock.data.command.CommandData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOverloadData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.java.ServerDefineCommandsEvent;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.event.GeyserEventBus;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CommandTranslator {

    private CommandTranslator() {}

    public static List<CommandData> translate(GeyserSession session, CommandNode[] nodes, int firstNodeIndex) {
        JavaCommandContext javaContext = null;
        BedrockCommandContext bedrockContext = null;

        CommandNode rootNode = nodes[firstNodeIndex];

        for (int nodeIndex : rootNode.getChildIndices()) {
            CommandNode node = nodes[nodeIndex];

            BedrockCommandInfo translated = translate(javaContext, node);
            bedrockContext.commands.computeIfAbsent(translated, $ -> new HashSet<>()).add(translated.name());

            // Add the command to the command lists - we have to use the Java name here as this is used to compare to Java commands.
            bedrockContext.knownCommands.add(node.getName());
            if (node.isAllowsRestricted()) { // Name is a bit confusing - this is what we want
                bedrockContext.restrictedCommands.add(node.getName());
            }
        }

        GeyserEventBus eventBus = session.getGeyser().eventBus();

        ServerDefineCommandsEvent event = new ServerDefineCommandsEvent(session, bedrockContext.commands.keySet());
        eventBus.fire(event);
        if (event.isCancelled()) {
            return List.of();
        }

        var oldEvent = new org.geysermc.geyser.api.event.downstream.ServerDefineCommandsEvent(session, bedrockContext.commands.keySet());
        eventBus.fire(oldEvent);
        if (oldEvent.isCancelled()) {
            return List.of();
        }

        // The command flags, set to NOT_CHEAT so known commands can be used while achievements are enabled.
        Set<CommandData.Flag> flags = Set.of(CommandData.Flag.NOT_CHEAT);

        boolean helpAdded = false;

        // These are the final commands sent to bedrock
        List<CommandData> bedrockCommands = new ObjectArrayList<>();

        // Loop through all the found commands
        for (Map.Entry<BedrockCommandInfo, Set<String>> entry : bedrockContext.commands.entrySet()) {
            String commandName = entry.getValue().iterator().next(); // We know this has a value

            LinkedHashMap<String, Set<CommandEnumConstraint>> values = new LinkedHashMap<>();
            // Is this right?
            for (String s : entry.getValue()) {
                values.put(s, EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES));
            }

            // Create a basic alias
            CommandEnumData aliases = new CommandEnumData(commandName + "Aliases", values, false);

            // Fetch command description
            String description = entry.getKey().description();

            // Command suggestion list is illegible if a command description contains line breaks
            int lineBreak = description.indexOf('\n');
            if (lineBreak >= 0) {
                description = description.substring(0, lineBreak);
            }

            // Since 1.21.130, the maximum description length is 1000 characters
            // https://www.minecraft.net/en-us/article/minecraft-1-21-130-bedrock-changelog
            // As issues are still experienced at that length, truncate descriptions at 950 characters
            if (description.length() > 950) {
                description = description.substring(0, 947) + "...";
            }

            // Build the completed command and add it to the final list
            CommandData data = new CommandData(commandName, description, flags, CommandPermission.ANY, aliases, Collections.emptyList(), entry.getKey().paramData());
            bedrockCommands.add(data);

            if (commandName.equals("help")) {
                helpAdded = true;
            }
        }

        if (!helpAdded) {
            // https://github.com/GeyserMC/Geyser/issues/2573 if Brigadier does not send the help command.
            bedrockCommands.add(createFakeHelpCommand());
        }

        if (session.getGeyser().platformType() == PlatformType.STANDALONE) {
            session.getGeyser().commandRegistry().export(session, bedrockCommands, bedrockContext.knownCommands);
        }

        return bedrockCommands;
    }

    private static BedrockCommandInfo translate(JavaCommandContext javaContext, CommandNode node) {
        assert node.getType() == CommandType.LITERAL : "origin command node must be literal";

        // Get and parse all params
        CommandOverloadData[] params = getParams(javaContext, node);

        // Insert the alias name into the command list
        String name = node.getName().toLowerCase(Locale.ROOT);
        String description = javaContext.registry.description(name, javaContext.locale);

        return new BedrockCommandInfo(name, description, params);
    }

    /**
     * Build the command parameter array for the given command
     *
     * @param context the {@link JavaCommandContext}
     * @param commandNode The command to build the parameters for
     * @return An array of parameter option arrays
     */
    private static CommandOverloadData[] getParams(JavaCommandContext context, CommandNode commandNode) {
        // Check if the command is an alias and redirect it
        if (commandNode.getRedirectIndex().isPresent()) {
            int redirectIndex = commandNode.getRedirectIndex().getAsInt();
            // FIXME perf
            GeyserImpl.getInstance().getLogger().debug("Redirecting command " + commandNode.getName() + " to " + context.nodes[redirectIndex].getName());
            commandNode = context.nodes[redirectIndex];
        }

        if (commandNode.getChildIndices().length >= 1) {
            // Create the root param node and build all the children
            ParamInfo rootParam = new ParamInfo(commandNode, null);
            rootParam.buildChildren(context);

            List<CommandOverloadData> treeData = rootParam.getTree();

            return treeData.toArray(new CommandOverloadData[0]);
        }

        return new CommandOverloadData[0];
    }

    private static CommandData createFakeHelpCommand() {
        CommandEnumData aliases = new CommandEnumData("helpAliases", Map.of("help", EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES)), false);
        return new CommandData("help", "", Set.of(CommandData.Flag.NOT_CHEAT), CommandPermission.ANY, aliases, Collections.emptyList(), new CommandOverloadData[0]);
    }
}
