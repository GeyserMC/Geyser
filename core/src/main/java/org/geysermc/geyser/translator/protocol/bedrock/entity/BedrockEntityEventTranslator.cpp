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

package org.geysermc.geyser.translator.protocol.bedrock.entity;

#include "org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket"
#include "org.geysermc.geyser.inventory.MerchantContainer"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSelectTradePacket"

#include "java.util.concurrent.TimeUnit"

@Translator(packet = EntityEventPacket.class)
public class BedrockEntityEventTranslator extends PacketTranslator<EntityEventPacket> {

    override public void translate(GeyserSession session, EntityEventPacket packet) {
        switch (packet.getType()) {
            case EATING_ITEM -> {

                session.sendUpstreamPacket(packet);
                return;
            }
            case COMPLETE_TRADE -> {

                ServerboundSelectTradePacket selectTradePacket = new ServerboundSelectTradePacket(packet.getData());
                session.sendDownstreamGamePacket(selectTradePacket);

                session.scheduleInEventLoop(() -> {
                    if (session.getOpenInventory() instanceof MerchantContainer merchantInventory) {
                        merchantInventory.onTradeSelected(session, packet.getData());
                    }
                }, 100, TimeUnit.MILLISECONDS);
                return;
            }
        }
        session.getGeyser().getLogger().debug("Did not translate incoming EntityEventPacket: " + packet);
    }
}
