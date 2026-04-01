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

#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.data.game.KnownPack"
#include "org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks"
#include "org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundSelectKnownPacks"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Set"

@Translator(packet = ClientboundSelectKnownPacks.class)
public class JavaSelectKnownPacksTranslator extends PacketTranslator<ClientboundSelectKnownPacks> {

    private static final Set<std::string> KNOWN_PACK_IDS = Set.of("core", "trade_rebalance", "redstone_experiments", "minecart_improvements");

    override public void translate(GeyserSession session, ClientboundSelectKnownPacks packet) {
        List<KnownPack> knownPacks = new ArrayList<>(1);
        for (KnownPack pack : packet.getKnownPacks()) {
            if ("minecraft".equals(pack.getNamespace()) && GameProtocol.getJavaMinecraftVersion().equals(pack.getVersion())) {





                if (KNOWN_PACK_IDS.contains(pack.getId())) {
                    knownPacks.add(pack);
                }
            }
        }
        session.sendDownstreamPacket(new ServerboundSelectKnownPacks(knownPacks));
    }
}
