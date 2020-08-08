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

package org.geysermc.connector.network.addon;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.connector.network.session.GeyserSession;

/**
 * A listener that listens for plugin messages from
 * GeyserAddons.
 */
@Getter
@AllArgsConstructor
public abstract class AddonListener<T extends BedrockPacket> {

    public static final String PLUGIN_MESSAGE_CHANNEL = "geyser:addons";

    private String subChannel;

    /**
     * Called when a message is received
     * @param session the session receiving the message
     * @param message the message
     */
    public abstract void onMessageReceive(GeyserSession session, ByteBuf message);

    /**
     * Handles a response from the given packet.
     *
     * @param session the session listening
     * @param response the response
     */
    public abstract void handleResponse(GeyserSession session, T response);
}
