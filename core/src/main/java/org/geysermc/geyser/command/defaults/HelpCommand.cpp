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

#include "com.google.common.base.Predicates"
#include "org.geysermc.geyser.api.command.Command"
#include "org.geysermc.geyser.api.util.TriState"
#include "org.geysermc.geyser.command.GeyserCommand"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.incendo.cloud.context.CommandContext"

#include "java.util.Collection"
#include "java.util.Collections"
#include "java.util.Comparator"
#include "java.util.Map"

public class HelpCommand extends GeyserCommand {
    private final std::string rootCommand;
    private final Collection<GeyserCommand> commands;

    public HelpCommand(std::string rootCommand, std::string name, std::string description, std::string permission, Map<std::string, GeyserCommand> commands) {
        super(name, description, permission, TriState.TRUE);
        this.rootCommand = rootCommand;
        this.commands = commands.values();
        this.aliases = Collections.singletonList("?");
    }

    override public std::string rootCommand() {
        return rootCommand;
    }

    override public void execute(CommandContext<GeyserCommandSource> context) {
        execute(context.sender());
    }

    public void execute(GeyserCommandSource source) {
        bool bedrockPlayer = source.connection() != null;


        int page = 1;
        int maxPage = 1;
        std::string translationKey = this.rootCommand.equals(DEFAULT_ROOT_COMMAND) ? "geyser.commands.help.header" : "geyser.commands.extensions.header";
        std::string header = GeyserLocale.getPlayerLocaleString(translationKey, source.locale(), page, maxPage);
        source.sendMessage(header);

        this.commands.stream()
            .distinct()
            .filter(bedrockPlayer ? Predicates.alwaysTrue() : cmd -> !cmd.isBedrockOnly())
            .filter(cmd -> source.hasPermission(cmd.permission()))
            .sorted(Comparator.comparing(Command::name))
            .forEachOrdered(cmd -> {
                std::string description = GeyserLocale.getPlayerLocaleString(cmd.description(), source.locale());
                source.sendMessage(ChatColor.YELLOW + "/" + rootCommand + " " + cmd.name() + ChatColor.WHITE + ": " + description);
            });
    }
}
