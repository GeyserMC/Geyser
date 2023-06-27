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

package org.geysermc.geyser.command.defaults;

import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class HelpCommand extends GeyserCommand {
    private final String baseCommand;
    private final Collection<Command> commands;

    public HelpCommand(GeyserImpl geyser, String name, String description, String permission,
                       String baseCommand, Map<String, Command> commands) {
        super(name, description, permission);
        this.baseCommand = baseCommand;
        this.commands = commands.values();

        this.aliases(Collections.singletonList("?"));
    }

    @Override
    public cloud.commandframework.Command.Builder<GeyserCommandSource> builder(CommandManager<GeyserCommandSource> manager) {
        return super.builder(manager)
            .handler(this::execute);
    }

    private void execute(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.getSender();
        boolean bedrockPlayer = source.connection().isPresent();

        // todo: pagination
        int page = 1;
        int maxPage = 1;
        String header = GeyserLocale.getPlayerLocaleString("geyser.commands.help.header", source.locale(), page, maxPage);
        source.sendMessage(header);

        this.commands.stream()
            .distinct() // remove aliases
            .sorted(Comparator.comparing(Command::name))
            .filter(cmd -> source.hasPermission(cmd.permission()))
            .filter(cmd -> !cmd.isBedrockOnly() || bedrockPlayer) // remove bedrock only commands if not a bedrock player
            .forEach(cmd -> source.sendMessage(ChatColor.YELLOW + "/" + baseCommand + " " + cmd.name() + ChatColor.WHITE + ": " +
                GeyserLocale.getPlayerLocaleString(cmd.description(), source.locale())));
    }
}
