/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.sponge.command;

import net.kyori.adventure.text.Component;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandExecutor;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GeyserSpongeCommandExecutor extends CommandExecutor implements Command.Raw {

    public GeyserSpongeCommandExecutor(GeyserConnector connector) {
        super(connector);
    }

    @Override
    public CommandResult process(CommandCause source, ArgumentReader.Mutable arguments) {
        CommandSender commandSender = new SpongeCommandSender(source);
        GeyserSession session = getGeyserSession(commandSender);

        String[] args = arguments.input().split(" "); //todo: this probably doesn't work
        if (args.length > 0) {
            GeyserCommand command = getCommand(args[0]);
            if (command != null) {
                if (!source.hasPermission(command.getPermission())) {
                    // Not ideal to use log here but we dont get a session
                    source.audience().sendMessage(Component.text(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.permission_fail"))); // todo: might not work
                    return CommandResult.success();
                }
                if (command.isBedrockOnly() && session == null) {
                    source.audience().sendMessage(Component.text(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.bedrock_only")));
                    return CommandResult.success();
                }
                getCommand(args[0]).execute(session, commandSender, args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
            }
        } else {
            getCommand("help").execute(session, commandSender, new String[0]);
        }
        return CommandResult.success();
    }

    @Override
    public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) {
        if (arguments.input().split(" ").length == 1) {
            return tabComplete(new SpongeCommandSender(cause)).stream().map(CommandCompletion::of).collect(Collectors.toList());
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
