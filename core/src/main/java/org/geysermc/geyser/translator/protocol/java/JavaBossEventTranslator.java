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

package org.geysermc.geyser.translator.protocol.java;

import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BossBar;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundBossEventPacket.class)
public class JavaBossEventTranslator extends PacketTranslator<ClientboundBossEventPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundBossEventPacket packet) {
        BossBar bossBar = session.getEntityCache().getBossBar(packet.getUuid());
        switch (packet.getAction()) {
            case ADD:
                long entityId = session.getEntityCache().nextEntityId();
                bossBar = new BossBar(session, entityId, packet.getTitle(), packet.getHealth(), bedrockColor(packet.getColor()), 1, 0);
                session.getEntityCache().addBossBar(packet.getUuid(), bossBar);
                break;
            case UPDATE_TITLE:
                if (bossBar != null) bossBar.updateTitle(packet.getTitle());
                break;
            case UPDATE_HEALTH:
                if (bossBar != null) bossBar.updateHealth(packet.getHealth());
                break;
            case REMOVE:
                session.getEntityCache().removeBossBar(packet.getUuid());
                break;
            case UPDATE_STYLE:
                if (bossBar != null) bossBar.updateColor(bedrockColor(packet.getColor()));
                break;
            case UPDATE_FLAGS:
                //todo
        }
    }

    /** Bedrock id 6 is REBECCA_PURPLE, which has no Java equivalent */
    private static int bedrockColor(BossBarColor color) {
        return switch (color) {
            case PINK -> 0;
            case CYAN -> 1;
            case RED -> 2;
            case LIME -> 3;
            case YELLOW -> 4;
            case PURPLE -> 5;
            case WHITE -> 7;
        };
    }
}
