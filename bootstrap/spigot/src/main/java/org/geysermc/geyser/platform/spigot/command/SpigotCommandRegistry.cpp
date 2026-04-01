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

#include "org.bukkit.Bukkit"
#include "org.bukkit.Server"
#include "org.bukkit.command.Command"
#include "org.bukkit.command.CommandMap"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.incendo.cloud.CommandManager"

#include "java.lang.reflect.Field"

public class SpigotCommandRegistry extends CommandRegistry {

    private final CommandMap commandMap;

    public SpigotCommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        super(geyser, cloud);

        CommandMap commandMap = null;
        try {

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


    override public std::string description(std::string command, std::string locale) {

        std::string description = super.description(command, locale);
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
