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

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.cloudburstmc.protocol.bedrock.data.command.CommandData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOverloadData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.java.ServerDefineCommandsEvent;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.translator.data.CommandTranslationData;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CommandTranslator {

    private CommandTranslator() {}

    @SuppressWarnings("removal") // Legacy API event
    public static List<CommandData> translate(CommandTranslationData data) {
        BedrockCommandData bedrockData = new BedrockCommandData();

        CommandNode rootNode = data.nodes()[data.firstNodeIndex()];
        for (int nodeIndex : rootNode.getChildIndices()) {
            CommandNode command = data.nodes()[nodeIndex];
            translate(data, bedrockData, command);
        }

        ServerDefineCommandsEvent event = data.fireConnectionEvent(connection -> new ServerDefineCommandsEvent(connection, bedrockData.commands.keySet()));
        if (event.isCancelled()) {
            return List.of();
        }

        var oldEvent = data.fireConnectionEvent(connection -> new org.geysermc.geyser.api.event.downstream.ServerDefineCommandsEvent(connection, bedrockData.commands.keySet()));
        if (oldEvent.isCancelled()) {
            return List.of();
        }

        // The command flags, set to NOT_CHEAT so known commands can be used while achievements are enabled.
        Set<CommandData.Flag> flags = Set.of(CommandData.Flag.NOT_CHEAT);

        boolean helpAdded = false;

        // These are the final commands sent to bedrock
        List<CommandData> bedrockCommands = new ObjectArrayList<>();

        // Loop through all the found commands, constructing a CommandData entry for each of them
        // These are the final entries sent to bedrock
        for (Map.Entry<BedrockCommandInfo, Set<String>> translation : bedrockData.commands.entrySet()) {
            BedrockCommandInfo commandInfo = translation.getKey();
            Set<String> aliasNames = translation.getValue();

            String commandName = commandInfo.name();

            Map<String, Set<CommandEnumConstraint>> aliasConstraints = new Object2ObjectLinkedOpenHashMap<>();
            // Command name itself has to be an alias too, the first presumably
            aliasConstraints.put(commandName, Set.of());
            // Register all the similar commands we found as alias
            aliasNames.forEach(alias -> aliasConstraints.put(alias, Set.of()));
            // Finally, also add Java's explicit redirects as alias, if there were any
            bedrockData.redirects.getOrDefault(commandName, Set.of()).forEach(redirect -> aliasConstraints.put(redirect, Set.of()));

            // Create the aliases enum
            CommandEnumData aliases = new CommandEnumData(commandName + "Aliases", aliasConstraints, false);

            // Fetch command description
            String description = commandInfo.description();

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
            CommandData bedrockCommand = new CommandData(commandName, description, flags, CommandPermission.ANY, aliases, Collections.emptyList(), commandInfo.paramData());
            bedrockCommands.add(bedrockCommand);

            if (commandName.equals("help")) {
                helpAdded = true;
            }
        }

        if (!helpAdded) {
            // https://github.com/GeyserMC/Geyser/issues/2573 if Brigadier does not send the help command.
            bedrockCommands.add(createFakeHelpCommand());
        }

        if (data.platformType() == PlatformType.STANDALONE) {
            data.commandRegistry().export(data.commandSource(), bedrockCommands, bedrockData.knownCommands);
        }

        return bedrockCommands;
    }

    private static void translate(CommandTranslationData data, BedrockCommandData bedrockData, CommandNode command) {
        assert command.getType() == CommandType.LITERAL : "origin command node must be literal";
        assert command.getName() != null; // Because type == LITERAL

        // Add the command to the command lists - we have to use the Java name here as this is used to compare to Java commands.
        bedrockData.knownCommands.add(command.getName());
        if (command.isAllowsRestricted()) { // Name is a bit confusing - this is what we want
            bedrockData.restrictedCommands.add(command.getName());
        }

        String bedrockName = command.getName().toLowerCase(Locale.ROOT);
        String description = data.commandRegistry().description(bedrockName, data.locale());

        // Redirect origin node -> this command is an alias of another command
        if (command.getRedirectIndex().isPresent()) {
            // TODO this will break if the redirect isn't to another origin
            CommandNode destination = data.nodes()[command.getRedirectIndex().orElseThrow()];
            assert destination.getName() != null : "redirect destination node must have a name";
            // destination name to lower case as for all bedrock command names
            bedrockData.redirects.computeIfAbsent(destination.getName().toLowerCase(Locale.ROOT), $ -> new ObjectOpenHashSet<>()).add(bedrockName);
            return;
        }

        CommandOverloadData[] params = getParams(data, command);
        BedrockCommandInfo bedrockCommandInfo = new BedrockCommandInfo(bedrockName, description, params);
        bedrockData.commands.computeIfAbsent(bedrockCommandInfo, $ -> new ObjectOpenHashSet<>()).add(bedrockName);
    }

    /**
     * Build the command parameter array for the given command
     *
     * @param data the {@link CommandTranslationData}
     * @param command The command to build the parameters for
     * @return An array of parameter option arrays
     */
    private static CommandOverloadData[] getParams(CommandTranslationData data, CommandNode command) {
        // Check if the command is an alias and redirect it
        if (command.getRedirectIndex().isPresent()) {
            throw new IllegalStateException("Can't translate a redirect");
            // FIXME can be removed?
            /*int redirectIndex = command.getRedirectIndex().getAsInt();
            // FIXME perf
            GeyserImpl.getInstance().getLogger().debug("Redirecting command " + command.getName() + " to " + data.nodes()[redirectIndex].getName());
            command = data.nodes()[redirectIndex];*/
        }

        // This command has children -> extra args can be given
        if (command.getChildIndices().length > 0) {
            // Create the root param node and build all the children (args)
            CommandParamInfo rootParam = new CommandParamInfo(command);
            rootParam.buildChildren(data);

            // TODO why are we returning an array?
            return rootParam.getTree().toArray(new CommandOverloadData[0]);
        }

        return new CommandOverloadData[0];
    }

    public static CommandData createFakeHelpCommand() {
        CommandEnumData aliases = new CommandEnumData("helpAliases", Map.of("help", EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES)), false);
        return new CommandData("help", "", Set.of(CommandData.Flag.NOT_CHEAT), CommandPermission.ANY, aliases, Collections.emptyList(), new CommandOverloadData[0]);
    }
}
