/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.spigot.command;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.incendo.cloud.CommandManager;

import java.lang.reflect.Field;

public class SpigotCommandRegistry extends CommandRegistry {

    private final CommandMap commandMap;

    public SpigotCommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        super(geyser, cloud);

        CommandMap commandMap = null;
        try {
            // Paper-only
            Server.class.getMethod("getCommandMap");
            commandMap = Bukkit.getServer().getCommandMap();
        } catch (NoSuchMethodException e) {
            try {
                Field cmdMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                cmdMapField.setAccessible(true);
                commandMap = (CommandMap) cmdMapField.get(Bukkit.getServer());
            } catch (Exception ex) {
                geyser.getLogger().error("Failed to get Spigot's CommandMap", ex);
            }
        }
        this.commandMap = commandMap;
    }

    @NonNull
    @Override
    public String description(@NonNull String command, @NonNull String locale) {
        // check if the command is /geyser or an extension command so that we can localize the description
        String description = super.description(command, locale);
        if (!description.isBlank()) {
            return description;
        }

        if (commandMap != null) {
            Command cmd = commandMap.getCommand(command);
            if (cmd != null) {
                return cmd.getDescription();
            }
        }
        return "";
    }
}
