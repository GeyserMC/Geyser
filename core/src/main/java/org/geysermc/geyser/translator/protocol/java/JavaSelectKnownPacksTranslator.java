/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.java;

import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.mcprotocollib.protocol.data.game.KnownPack;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundSelectKnownPacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Translator(packet = ClientboundSelectKnownPacks.class)
public class JavaSelectKnownPacksTranslator extends PacketTranslator<ClientboundSelectKnownPacks> {
    // todo: dump from client?
    private static final Set<String> KNOWN_PACK_IDS = Set.of("core", "winter_drop", "trade_rebalance", "redstone_experiments", "minecart_improvements");

    @Override
    public void translate(GeyserSession session, ClientboundSelectKnownPacks packet) {
        List<KnownPack> knownPacks = new ArrayList<>(1);
        for (KnownPack pack : packet.getKnownPacks()) {
            if ("minecraft".equals(pack.getNamespace()) && GameProtocol.getJavaMinecraftVersion().equals(pack.getVersion())) {
                // Implementation note: these won't always necessarily be equal.
                // 1.20.5 versus 1.20.6 for example - same protocol version. A vanilla server for either version will gracefully accept either.
                // If the versions mismatch, the registry will be sent over the network, though.
                // For vanilla compliancy, to minimize network traffic, and for potential future behavior,
                // We'll implement how the Java client does it.
                if (KNOWN_PACK_IDS.contains(pack.getId())) {
                    knownPacks.add(pack);
                }
            }
        }
        session.sendDownstreamPacket(new ServerboundSelectKnownPacks(knownPacks));
    }

    @Override
    public boolean shouldExecuteInEventLoop() {
        // This technically isn't correct behavior, but it prevents race conditions between MCProtocolLib's packet handler and ours.
        return false;
    }
}
