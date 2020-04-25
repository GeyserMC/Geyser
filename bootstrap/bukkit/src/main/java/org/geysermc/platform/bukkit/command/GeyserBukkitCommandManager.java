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

package org.geysermc.platform.bukkit.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.platform.bukkit.GeyserBukkitPlugin;

import java.lang.reflect.Field;

public class GeyserBukkitCommandManager extends CommandManager {

    private static CommandMap COMMAND_MAP;

    static {
        try {
            Field cmdMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmdMapField.setAccessible(true);
            COMMAND_MAP = (CommandMap) cmdMapField.get(Bukkit.getServer());
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    private GeyserBukkitPlugin plugin;

    public GeyserBukkitCommandManager(GeyserBukkitPlugin plugin, GeyserConnector connector) {
        super(connector);

        this.plugin = plugin;
    }

    @Override
    public String getDescription(String command) {
        Command cmd = COMMAND_MAP.getCommand(command.replace("/", ""));
        return cmd != null ? cmd.getDescription() : "";
    }
}
