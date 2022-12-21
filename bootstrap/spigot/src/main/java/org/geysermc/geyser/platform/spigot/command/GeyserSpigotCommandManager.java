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

package org.geysermc.geyser.platform.spigot.command;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.GeyserCommandManager;

import java.lang.reflect.Field;

public class GeyserSpigotCommandManager extends GeyserCommandManager {

    private static final CommandMap COMMAND_MAP;

    static {
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
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        COMMAND_MAP = commandMap;
    }

    public GeyserSpigotCommandManager(GeyserImpl geyser) {
        super(geyser);
    }

    @Override
    public String description(String command) {
        Command cmd = COMMAND_MAP.getCommand(command.replace("/", ""));
        return cmd != null ? cmd.getDescription() : "";
    }

    public static CommandMap getCommandMap() {
        return COMMAND_MAP;
    }
}
