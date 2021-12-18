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

package org.geysermc.geyser.command;

import lombok.AllArgsConstructor;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents helper functions for listening to {@code /geyser} commands.
 */
@AllArgsConstructor
public class CommandExecutor {

    protected final GeyserImpl geyser;

    public GeyserCommand getCommand(String label) {
        return geyser.getCommandManager().getCommands().get(label);
    }

    @Nullable
    public GeyserSession getGeyserSession(CommandSender sender) {
        if (sender.isConsole()) {
            return null;
        }

        for (GeyserSession session : geyser.getSessionManager().getSessions().values()) {
            if (sender.name().equals(session.getPlayerEntity().getUsername())) {
                return session;
            }
        }
        return null;
    }

    /**
     * Determine which subcommands to suggest in the tab complete for the main /geyser command by a given command sender.
     *
     * @param sender The command sender to receive the tab complete suggestions.
     *               If the command sender is a bedrock player, an empty list will be returned as bedrock players do not get command argument suggestions.
     *               If the command sender is not a bedrock player, bedrock commands will not be shown.
     *               If the command sender does not have the permission for a given command, the command will not be shown.
     * @return A list of command names to include in the tab complete
     */
    public List<String> tabComplete(CommandSender sender) {
        if (getGeyserSession(sender) != null) {
            // Bedrock doesn't get tab completions or argument suggestions
            return Collections.emptyList();
        }

        List<String> availableCommands = new ArrayList<>();
        Map<String, GeyserCommand> commands = geyser.getCommandManager().getCommands();

        // Only show commands they have permission to use
        for (Map.Entry<String, GeyserCommand> entry : commands.entrySet()) {
            GeyserCommand geyserCommand = entry.getValue();
            if (sender.hasPermission(geyserCommand.getPermission())) {

                if (geyserCommand.isBedrockOnly()) {
                    // Don't show commands the JE player can't run
                    continue;
                }

                availableCommands.add(entry.getKey());
            }
        }

        return availableCommands;
    }
}
