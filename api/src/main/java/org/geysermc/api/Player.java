/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.api;

import org.geysermc.api.command.CommandSender;
import org.geysermc.api.session.AuthData;
import org.geysermc.api.window.FormWindow;

import java.net.InetSocketAddress;

public interface Player extends CommandSender {

    /**
     * Connects the player to the remote server
     *
     * @param remoteServer the remote server to connect to
     */
    void connect(RemoteServer remoteServer);

    /**
     * Disconnect the player for the specified reason
     *
     * @param reason the reason to disconnect the player for
     */
    void disconnect(String reason);

    /**
     * Returns the authentication data of the player. This is not the
     * player's Minecraft credentials; it's simply what is given to the server
     * (Name, UUID, Xbox UUID) to verify the player can/actually exists.
     *
     * @return the authentication data of the player
     */
    AuthData getAuthenticationData();

    /**
     * Sends a form window
     *
     * @param window the window form to send
     */
    void sendForm(FormWindow window);

    /**
     * Sends a form window with the given ID
     *
     * @param window the window to send
     * @param id the id of the window
     */
    void sendForm(FormWindow window, int id);

    /**
     * Returns the current hostname and port the player is connected with.
     *
     * @return player's socket address.
     */
    InetSocketAddress getSocketAddress();
}
