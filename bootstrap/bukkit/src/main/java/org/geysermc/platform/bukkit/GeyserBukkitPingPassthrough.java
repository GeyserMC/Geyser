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

package org.geysermc.platform.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.geysermc.common.IGeyserPingPassthrough;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class GeyserBukkitPingPassthrough implements IGeyserPingPassthrough {

    @Override
    public String getMOTD() {
        try {
            // https://bukkit.org/threads/get-the-motd-of-a-server-in-game.251590/
            Socket sock = new Socket();
            sock.setSoTimeout(100);
            sock.connect(new InetSocketAddress(Bukkit.getIp(), Bukkit.getPort()), 100);

            DataOutputStream out = new DataOutputStream(sock.getOutputStream());
            DataInputStream in = new DataInputStream(sock.getInputStream());

            out.write(0xFE);

            int b;
            StringBuilder str = new StringBuilder();
            while ((b = in .read()) != -1) {
                if (b > 16 && b != 255 && b != 23 && b != 24) {
                    str.append((char) b);
                }
            }

            sock.close();

            // Remove first indicator of player count
            String MOTD = str.toString().substring(1).substring(0, str.toString().lastIndexOf(ChatColor.COLOR_CHAR));
            // Remove second indicator of player count
            return MOTD.substring(0, MOTD.lastIndexOf(ChatColor.COLOR_CHAR));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
        //return Bukkit.getMotd(); Quick way to get MOTD.
    }

    @Override
    public int getMaxPlayerCount() {
        return Bukkit.getMaxPlayers();
    }

    @Override
    public int getCurrentPlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }
}
