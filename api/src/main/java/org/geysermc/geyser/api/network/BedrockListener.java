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

package org.geysermc.geyser.api.network;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The listener that handles connections from Minecraft:
 * Bedrock Edition.
 */
public interface BedrockListener {

    /**
     * Gets the address used for listening for Bedrock
     * connections from.
     *
     * @return the listening address
     */
    @NonNull
    String address();

    /**
     * Gets the port used for listening for Bedrock
     * connections from.
     *
     * @return the listening port
     */
    int port();

    /**
     * Gets the broadcast port that's sent to Bedrock clients with the motd.
     * This is the port that Bedrock clients will connect with. It usually does not differ from the listening port.
     *
     * @return the broadcast port
     */
    int broadcastPort();

    /**
     * Gets the primary MOTD shown to Bedrock players if a ping passthrough setting is not enabled.
     * <p>
     * This is the first line that will be displayed.
     *
     * @return the primary MOTD shown to Bedrock players.
     */
    String primaryMotd();

    /**
     * Gets the secondary MOTD shown to Bedrock players if a ping passthrough setting is not enabled.
     * <p>
     * This is the second line that will be displayed.
     *
     * @return the secondary MOTD shown to Bedrock players.
     */
    String secondaryMotd();

    /**
     * Gets the server name that is sent to Bedrock clients.
     *
     * @return the server sent to Bedrock clients
     */
    String serverName();
}
