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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.ItemFrameDropItemPacket;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = ItemFrameDropItemPacket.class)
public class BedrockItemFrameDropItemTranslator extends PacketTranslator<ItemFrameDropItemPacket> {

    @Override
    public void translate(ItemFrameDropItemPacket packet, GeyserSession session) {
        // I hope that, when we die, God (or whoever is waiting for us) tells us exactly why this code exists
        // The packet sends the Y coordinate (and just the Y coordinate) divided by two, and it's negative if it needs to be subtracted by one
        int y;
        if (packet.getBlockPosition().getY() > 0) {
            y = packet.getBlockPosition().getY() * 2;
        } else {
            y = (packet.getBlockPosition().getY() * -2) - 1;
        }
        Vector3i position = Vector3i.from(packet.getBlockPosition().getX(), y, packet.getBlockPosition().getZ());
        ClientPlayerInteractEntityPacket interactPacket = new ClientPlayerInteractEntityPacket((int) ItemFrameEntity.getItemFrameEntityId(session, position),
                InteractAction.ATTACK, Hand.MAIN_HAND, session.isSneaking());
        session.sendDownstreamPacket(interactPacket);
    }

}
