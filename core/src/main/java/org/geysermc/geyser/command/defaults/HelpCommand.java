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

import com.google.common.base.Predicates;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.incendo.cloud.context.CommandContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class HelpCommand extends GeyserCommand {
    private final String rootCommand;
    private final Collection<GeyserCommand> commands;

    public HelpCommand(String rootCommand, String name, String description, String permission, Map<String, GeyserCommand> commands) {
        super(name, description, permission, TriState.TRUE);
        this.rootCommand = rootCommand;
        this.commands = commands.values();
        this.aliases = Collections.singletonList("?");
    }

    @Override
    public String rootCommand() {
        return rootCommand;
    }

    @Override
    public void execute(CommandContext<GeyserCommandSource> context) {
        execute(context.sender());
    }

    public void execute(GeyserCommandSource source) {
        boolean bedrockPlayer = source.connection() != null;

        // todo: pagination
        int page = 1;
        int maxPage = 1;
        String translationKey = this.rootCommand.equals(DEFAULT_ROOT_COMMAND) ? "geyser.commands.help.header" : "geyser.commands.extensions.header";
        String header = GeyserLocale.getPlayerLocaleString(translationKey, source.locale(), page, maxPage);
        source.sendMessage(header);

        this.commands.stream()
            .distinct() // remove aliases
            .filter(bedrockPlayer ? Predicates.alwaysTrue() : cmd -> !cmd.isBedrockOnly()) // remove bedrock only commands if not a bedrock player
            .filter(cmd -> source.hasPermission(cmd.permission()))
            .sorted(Comparator.comparing(Command::name))
            .forEachOrdered(cmd -> {
                String description = GeyserLocale.getPlayerLocaleString(cmd.description(), source.locale());
                source.sendMessage(ChatColor.YELLOW + "/" + rootCommand + " " + cmd.name() + ChatColor.WHITE + ": " + description);
            });
    }
}
