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

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandExecutor;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class GeyserSpongeCommandExecutor extends GeyserCommandExecutor implements CommandCallable {

    public GeyserSpongeCommandExecutor(GeyserImpl geyser, Map<String, Command> commands) {
        super(geyser, commands);
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) {
        GeyserCommandSource commandSender = new SpongeCommandSource(source);
        GeyserSession session = getGeyserSession(commandSender);

        String[] args = arguments.split(" ");
        if (args.length > 0) {
            GeyserCommand command = getCommand(args[0]);
            if (command != null) {
                if (!source.hasPermission(command.permission())) {
                    // Not ideal to use log here but we dont get a session
                    source.sendMessage(Text.of(ChatColor.RED + GeyserLocale.getLocaleStringLog("geyser.bootstrap.command.permission_fail")));
                    return CommandResult.success();
                }
                if (command.isBedrockOnly() && session == null) {
                    source.sendMessage(Text.of(ChatColor.RED + GeyserLocale.getLocaleStringLog("geyser.bootstrap.command.bedrock_only")));
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
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
        if (arguments.split(" ").length == 1) {
            return tabComplete(new SpongeCommandSource(source));
        }
        return Collections.emptyList();
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
