/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.bedrock.nethernet;

import io.netty.channel.Channel;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.util.nethernet.PlayerInfo;

import java.security.PublicKey;

/**
 * Ties the Bedrock login chain to the identity that opened the transport.
 * <p>
 * On RakNet the encryption handshake does this on its own: the session key is derived against the
 * chain's identity key, so only its holder can read what comes next. NetherNet runs over DTLS and
 * skips that handshake, which leaves the chain unbound, and a chain is replayable until something
 * binds it. The signalling assertion is what binds it here, because the peer proved it holds the key
 * the assertion names before the transport was accepted.
 *
 * @see <a href="https://github.com/Mojang/bedrock-protocol-docs/blob/main/NetherNetOnboardingGuide.md">NetherNet onboarding guide, section 5</a>
 */
public final class TransportIdentityBinding {

    private TransportIdentityBinding() {
    }

    /**
     * @param channel            the channel the login arrived on
     * @param identityPublicKey  the key the login chain is signed with
     * @return why the login must be rejected, or null when the two agree
     */
    public static String mismatch(Channel channel, PublicKey identityPublicKey) {
        if (!(channel instanceof NetherNetChildChannel)) {
            // RakNet binds the chain through the encryption handshake instead
            return null;
        }

        PlayerInfo player = channel.attr(NetherNetChildChannel.PLAYER_INFO).get();
        if (player == null) {
            return "the transport carries no validated identity to bind the login chain to";
        }

        try {
            if (!player.clientPublicKey().equals(identityPublicKey)) {
                return "the login chain is signed with a different key than the one that opened the transport";
            }
        } catch (Exception e) {
            return "the transport identity has no usable key: " + e.getMessage();
        }
        return null;
    }
}
