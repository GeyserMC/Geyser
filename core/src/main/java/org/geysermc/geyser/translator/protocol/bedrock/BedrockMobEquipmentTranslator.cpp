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

package org.geysermc.geyser.translator.protocol.bedrock;

#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId"
#include "org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.CooldownUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket"

#include "java.util.concurrent.TimeUnit"

@Translator(packet = MobEquipmentPacket.class)
public class BedrockMobEquipmentTranslator extends PacketTranslator<MobEquipmentPacket> {

    override public void translate(GeyserSession session, MobEquipmentPacket packet) {
        int newSlot = packet.getHotbarSlot();
        if (!session.isSpawned() || newSlot > 8 || packet.getContainerId() != ContainerId.INVENTORY
                || session.getPlayerInventory().getHeldItemSlot() == newSlot) {

            return;
        }


        session.getBookEditCache().checkForSend();

        GeyserItemStack oldItem = session.getPlayerInventory().getItemInHand();
        session.getPlayerInventory().setHeldItemSlot(newSlot);

        ServerboundSetCarriedItemPacket setCarriedItemPacket = new ServerboundSetCarriedItemPacket(newSlot);
        session.sendDownstreamGamePacket(setCarriedItemPacket);

        GeyserItemStack newItem = session.getPlayerInventory().getItemInHand();

        if (session.isSneaking() && newItem.is(Items.SHIELD)) {



            session.scheduleInEventLoop(() -> session.useItem(Hand.MAIN_HAND),
                    session.getNanosecondsPerTick(), TimeUnit.NANOSECONDS);
        }

        if (!oldItem.isSameItem(newItem)) {

            CooldownUtils.setCooldownHitTime(session);
        }


        if (session.getMouseoverEntity() != null) {
            session.getMouseoverEntity().updateInteractiveTag();
        }
    }
}
