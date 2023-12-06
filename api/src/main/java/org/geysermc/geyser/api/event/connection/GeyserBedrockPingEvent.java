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

package org.geysermc.geyser.api.event.connection;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.event.Event;

import java.net.InetSocketAddress;

/**
 * Called whenever Geyser gets pinged
 * <p>
 * This event allows you to modify/obtain the MOTD, maximum player count, and current number of players online,
 * Geyser will reply to the client with what was given.
 */
public interface GeyserBedrockPingEvent extends Event {

    /**
     * Sets the given string as the primary motd, the given string cannot be null.
     *
     * @param primary the string to set as the primary motd
     */
    void primaryMotd(@NonNull String primary);

    /**
     * Sets the given string as the secondary motd, the given string cannot be null.
     * Note: the secondary motd is only used for the LAN game entry.
     *
     * @param secondary the string to set as the secondary motd
     */
    void secondaryMotd(@NonNull String secondary);

    /**
     * Sets how many players are currently online, the given number cannot be below 0.
     *
     * @param count the number to set
     */
    void playerCount(int count);

    /**
     * Sets the maximum number of players that can join this server, the given number cannot be below 1.
     *
     * @param max the number to set
     */
    void maxPlayerCount(int max);

    /**
     * Gets the primary motd.
     *
     * @return the primary motd string
     */
    @Nullable
    String primaryMotd();

    /**
     * Gets the secondary motd.
     *
     * @return the secondary motd string
     */
    @Nullable
    String secondaryMotd();

    /**
     * Gets the current number of players.
     *
     * @return number of players online
     */
    @NonNegative
    int playerCount();

    /**
     * Gets the maximum number of players that can join this server
     *
     * @return maximum number of players that can join
     */
    int maxPlayerCount();

    /**
     * Gets the {@link InetSocketAddress} of the client pinging us.
     *
     * @return a {@link InetSocketAddress}
     */
    @NonNull
    InetSocketAddress address();
}
