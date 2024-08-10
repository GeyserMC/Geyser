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

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.CooldownUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;

import java.util.concurrent.TimeUnit;

@Translator(packet = MobEquipmentPacket.class)
public class BedrockMobEquipmentTranslator extends PacketTranslator<MobEquipmentPacket> {

    @Override
    public void translate(GeyserSession session, MobEquipmentPacket packet) {
        int newSlot = packet.getHotbarSlot();
        if (!session.isSpawned() || newSlot > 8 || packet.getContainerId() != ContainerId.INVENTORY
                || session.getPlayerInventory().getHeldItemSlot() == newSlot) {
            // For the last condition - Don't update the slot if the slot is the same - not Java Edition behavior and messes with plugins such as Grief Prevention
            return;
        }

        // Send book update before switching hotbar slot
        session.getBookEditCache().checkForSend();

        GeyserItemStack oldItem = session.getPlayerInventory().getItemInHand();
        session.getPlayerInventory().setHeldItemSlot(newSlot);

        ServerboundSetCarriedItemPacket setCarriedItemPacket = new ServerboundSetCarriedItemPacket(newSlot);
        session.sendDownstreamGamePacket(setCarriedItemPacket);

        GeyserItemStack newItem = session.getPlayerInventory().getItemInHand();

        if (session.isSneaking() && newItem.asItem() == Items.SHIELD) {
            // Activate shield since we are already sneaking
            // (No need to send a release item packet - Java doesn't do this when swapping items)
            // Required to do it a tick later or else it doesn't register
            session.scheduleInEventLoop(() -> session.useItem(Hand.MAIN_HAND),
                    session.getNanosecondsPerTick(), TimeUnit.NANOSECONDS);
        }

        if (oldItem.getJavaId() != newItem.getJavaId()) {
            // Java sends a cooldown indicator whenever you switch to a new item type
            CooldownUtils.sendCooldown(session);
        }

        // Update the interactive tag, if an entity is present
        if (session.getMouseoverEntity() != null) {
            session.getMouseoverEntity().updateInteractiveTag();
        }
    }
}
