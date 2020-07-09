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

package org.geysermc.platform.sponge.command;

import lombok.AllArgsConstructor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.command.GeyserCommand;
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

@AllArgsConstructor
public class GeyserSpongeCommandExecutor implements CommandCallable {

    private GeyserConnector connector;

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = arguments.split(" ");
        if (args.length > 0) {
            if (getCommand(args[0]) != null) {
                if (!source.hasPermission(getCommand(args[0]).getPermission())) {
                    // Not ideal to use log here but we dont get a session
                    source.sendMessage(Text.of(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.permission_fail")));
                    return CommandResult.success();
                }
                getCommand(args[0]).execute(new SpongeCommandSender(source), args);
            }
        } else {
            getCommand("help").execute(new SpongeCommandSender(source), args);
        }
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        if (arguments.split(" ").length == 1) {
            return Arrays.asList("?", "help", "reload", "shutdown", "stop");
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

    private GeyserCommand getCommand(String label) {
        return connector.getCommandManager().getCommands().get(label);
    }
}
