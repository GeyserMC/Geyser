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

#include "org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.PlayerListUtils"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.UUID"

@Translator(packet = ClientboundPlayerInfoRemovePacket.class)
public class JavaPlayerInfoRemoveTranslator extends PacketTranslator<ClientboundPlayerInfoRemovePacket> {
    override public void translate(GeyserSession session, ClientboundPlayerInfoRemovePacket packet) {
        List<PlayerListPacket.Entry> entries = new ArrayList<>();

        for (UUID id : packet.getProfileIds()) {

            PlayerEntity entity = session.getEntityCache().removePlayerEntity(id);
            if (entity != null) {

                if (!entity.isListed()) {
                    continue;
                }


                entries.add(new PlayerListPacket.Entry(entity.getTabListUuid()));
            }
        }

        if (!entries.isEmpty()) {
            PlayerListUtils.batchSendPlayerList(session, entries, PlayerListPacket.Action.REMOVE);
        }
    }
}
