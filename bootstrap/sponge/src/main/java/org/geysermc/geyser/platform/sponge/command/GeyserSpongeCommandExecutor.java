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

package org.geysermc.geyser.platform.sponge.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandExecutor;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;

import java.util.*;
import java.util.stream.Collectors;

public class GeyserSpongeCommandExecutor extends GeyserCommandExecutor implements org.spongepowered.api.command.Command.Raw {

    public GeyserSpongeCommandExecutor(GeyserImpl geyser, Map<String, Command> commands) {
        super(geyser, commands);
    }

    @Override
    public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) {
        GeyserCommandSource commandSource = new SpongeCommandSource(cause);
        GeyserSession session = getGeyserSession(commandSource);

        String[] args = arguments.input().split(" ");
        // This split operation results in an array of length 1, containing a zero length string, if the input string is empty
        if (args.length > 0 && !args[0].isEmpty()) {
            GeyserCommand command = getCommand(args[0]);
            if (command != null) {
                if (!cause.hasPermission(command.permission())) {
                    cause.audience().sendMessage(Component.text(GeyserLocale.getLocaleStringLog("geyser.bootstrap.command.permission_fail")).color(NamedTextColor.RED));
                    return CommandResult.success();
                }
                if (command.isBedrockOnly() && session == null) {
                    cause.audience().sendMessage(Component.text(GeyserLocale.getLocaleStringLog("geyser.bootstrap.command.bedrock_only")).color(NamedTextColor.RED));
                    return CommandResult.success();
                }
                command.execute(session, commandSource, args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
            } else {
                cause.audience().sendMessage(Component.text(GeyserLocale.getLocaleStringLog("geyser.bootstrap.command.not_found")).color(NamedTextColor.RED));
            }
        } else {
            getCommand("help").execute(session, commandSource, new String[0]);
        }
        return CommandResult.success();
    }

    @Override
    public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) {
        if (arguments.input().split(" ").length == 1) {
            return tabComplete(new SpongeCommandSource(cause)).stream().map(CommandCompletion::of).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canExecute(CommandCause cause) {
        return true;
    }

    @Override
    public Optional<Component> shortDescription(CommandCause cause) {
        return Optional.of(Component.text("The main command for Geyser."));
    }

    @Override
    public Optional<Component> extendedDescription(CommandCause cause) {
        return shortDescription(cause);
    }

    @Override
    public Optional<Component> help(@NotNull CommandCause cause) {
        return Optional.of(Component.text("/geyser help"));
    }

    @Override
    public Component usage(CommandCause cause) {
        return Component.text("/geyser help");
    }
}
