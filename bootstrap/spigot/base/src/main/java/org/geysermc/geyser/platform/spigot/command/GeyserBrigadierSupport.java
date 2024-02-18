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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.geysermc.geyser.platform.spigot.GeyserSpigotPlugin;

/**
 * Needs to be a separate class so pre-1.13 loads correctly.
 */
public final class GeyserBrigadierSupport {

    public static void loadBrigadier(GeyserSpigotPlugin plugin, PluginCommand pluginCommand) {
        // Enable command completions if supported
        // This is beneficial because this is sent over the network and Bedrock can see it
        Commodore commodore = CommodoreProvider.getCommodore(plugin);
        LiteralArgumentBuilder<?> builder = LiteralArgumentBuilder.literal("geyser");
        for (String command : plugin.getGeyserCommandManager().getCommands().keySet()) {
            builder.then(LiteralArgumentBuilder.literal(command));
        }
        commodore.register(pluginCommand, builder);

        try {
            Class.forName("com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent");
            Bukkit.getServer().getPluginManager().registerEvents(new GeyserPaperCommandListener(), plugin);
            plugin.getGeyserLogger().debug("Successfully registered AsyncPlayerSendCommandsEvent listener.");
        } catch (ClassNotFoundException e) {
            plugin.getGeyserLogger().debug("Not registering AsyncPlayerSendCommandsEvent listener.");
        }
    }

    private GeyserBrigadierSupport() {
    }
}
