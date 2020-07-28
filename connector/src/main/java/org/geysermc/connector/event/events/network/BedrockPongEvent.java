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

package org.geysermc.connector.event.events.network;

import com.nukkitx.protocol.bedrock.BedrockPong;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.geysermc.connector.event.GeyserEvent;

import java.net.InetSocketAddress;

/**
 * Triggered just before Geyser sends a Pong to a Ping packet
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("JavaDoc")
public class BedrockPongEvent extends GeyserEvent {
    /**
     * The connecting socket address
     *
     * @return get the connecting socket address
     */
    @NonNull
    private final InetSocketAddress inetSocketAddress;

    /**
     * The pong to broadcast
     *
     * This will initially be set to the Pong that Geyser would normally return but can be overridden here
     *
     * @param pong set the BedrockPong
     * @return get current pong
     */
    @NonNull
    private BedrockPong pong;
}
