/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.platform.bukkit.world;

import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.platform.bukkit.GeyserBukkitPlugin;

@AllArgsConstructor
public class GeyserBukkitPlayerListener implements Listener {

    GeyserConnector connector;
    boolean isLegacy; // Legacy is anything pre-1.12 before the declare recipes packet
    boolean isViaVersion;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (GeyserSession session : connector.getPlayers().values()) {
            if (event.getPlayer() == Bukkit.getPlayer(session.getPlayerEntity().getUsername())) {
                GeyserBukkitPlugin.getPlayerToSessionMap().put(event.getPlayer(), session);
                if (isLegacy && isViaVersion) {
                    GeyserBukkitLegacyCraftingTranslator.sendAllRecipes(session); // Send available recipes so crafting can work
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        GeyserBukkitPlugin.getPlayerToSessionMap().remove(event.getPlayer());
    }

}
