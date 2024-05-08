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

package org.geysermc.geyser.translator.protocol.java.entity.player;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import java.util.UUID;

@Translator(packet = ClientboundPlayerInfoRemovePacket.class)
public class JavaPlayerInfoRemoveTranslator extends PacketTranslator<ClientboundPlayerInfoRemovePacket> {
    @Override
    public void translate(GeyserSession session, ClientboundPlayerInfoRemovePacket packet) {
        PlayerListPacket translate = new PlayerListPacket();
        translate.setAction(PlayerListPacket.Action.REMOVE);

        for (UUID id : packet.getProfileIds()) {
            // As the player entity is no longer present, we can remove the entry
            PlayerEntity entity = session.getEntityCache().removePlayerEntity(id);
            UUID removeId;
            if (entity != null) {
                // Just remove the entity's player list status
                // Don't despawn the entity - the Java server will also take care of that.
                removeId = entity.getTabListUuid();
            } else {
                removeId = id;
            }
            translate.getEntries().add(new PlayerListPacket.Entry(removeId));
        }

        session.sendUpstreamPacket(translate);
    }
}
