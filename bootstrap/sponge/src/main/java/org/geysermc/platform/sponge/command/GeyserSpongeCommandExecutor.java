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

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandExecutor;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GeyserSpongeCommandExecutor extends CommandExecutor implements CommandCallable {

    public GeyserSpongeCommandExecutor(GeyserConnector connector) {
        super(connector);
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) {
        String[] args = arguments.split(" ");
        if (args.length > 0) {
            GeyserCommand command = getCommand(args[0]);
            if (command != null) {
                CommandSender commandSender = new SpongeCommandSender(source);
                if (!source.hasPermission(command.getPermission())) {
                    // Not ideal to use log here but we dont get a session
                    source.sendMessage(Text.of(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.permission_fail")));
                    return CommandResult.success();
                }
                GeyserSession session = null;
                if (command.isBedrockOnly()) {
                    session = getGeyserSession(commandSender);
                    if (session == null) {
                        source.sendMessage(Text.of(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.bedrock_only")));
                        return CommandResult.success();
                    }
                }
                getCommand(args[0]).execute(session, commandSender, args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
            }
        } else {
            getCommand("help").execute(null, new SpongeCommandSender(source), new String[0]);
        }
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        if (arguments.split(" ").length == 1) {
            return connector.getCommandManager().getCommandNames();
        }
        return new ArrayList<>();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return true;
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of("The main command for Geyser."));
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.of(Text.of("/geyser help"));
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("/geyser help");
    }
}
