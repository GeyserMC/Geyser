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

package org.geysermc.platform.spigot;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;

public class PlaceholderAPI extends PlaceholderExpansion {

    private GeyserSpigotPlugin plugin;

    public PlaceholderAPI(GeyserSpigotPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public String getAuthor(){
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier(){
        return "geyser";
    }

    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if(player == null) {
            return "";
        }

        switch (identifier) {
            case "minecraft_version":
                return GeyserConnector.MINECRAFT_VERSION;
            case "version":
                return GeyserConnector.VERSION;
            case "git_version":
                return GeyserConnector.GIT_VERSION;
            case "client_version":
                return getPlayerPlatform(player);
            case "platform":
                return getPlayerGameVersion(player);
        }

        return null;
    }

    /**
     * Get the platform of the specified player
     * @param player The player to get the platform for
     * @return The player's platform version
     */
    public String getPlayerPlatform(Player player) {
        GeyserSession geyserPlayer = GeyserConnector.getInstance().getPlayerByUuid(player.getUniqueId());

        if (geyserPlayer != null) {
            return geyserPlayer.getClientData().getDeviceOS().toString();
        } else {
            return "";
        }
    }

    /**
     * Get the game version of the specified player
     * @param player The player to get the game version for
     * @return The player's game version
     */
    public String getPlayerGameVersion(Player player) {
        GeyserSession geyserPlayer = GeyserConnector.getInstance().getPlayerByUuid(player.getUniqueId());

        if (geyserPlayer != null) {
            return geyserPlayer.getClientData().getGameVersion();
        } else {
            return "";
        }
    }
}
