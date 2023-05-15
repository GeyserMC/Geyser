/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import org.cloudburstmc.protocol.bedrock.packet.EmotePacket;
import org.geysermc.geyser.api.event.bedrock.ClientEmoteEvent;
import org.geysermc.geyser.configuration.EmoteOffhandWorkaroundOption;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = EmotePacket.class)
public class BedrockEmoteTranslator extends PacketTranslator<EmotePacket> {

    @Override
    public void translate(GeyserSession session, EmotePacket packet) {
        if (session.getGeyser().getConfig().getEmoteOffhandWorkaround() != EmoteOffhandWorkaroundOption.DISABLED) {
            // Activate the workaround - we should trigger the offhand now
            session.requestOffhandSwap();

            if (session.getGeyser().getConfig().getEmoteOffhandWorkaround() == EmoteOffhandWorkaroundOption.NO_EMOTES) {
                return;
            }
        }

        // For the future: could have a method that exposes which players will see the emote
        ClientEmoteEvent event = new ClientEmoteEvent(session, packet.getEmoteId());
        session.getGeyser().eventBus().fire(event);
        if (event.isCancelled()) {
            return;
        }

        int javaId = session.getPlayerEntity().getEntityId();
        for (GeyserSession otherSession : session.getGeyser().getSessionManager().getSessions().values()) {
            if (otherSession != session) {
                if (otherSession.isClosed()) continue;
                if (otherSession.getEventLoop().inEventLoop()) {
                    playEmote(otherSession, javaId, packet.getEmoteId());
                } else {
                    otherSession.executeInEventLoop(() -> playEmote(otherSession, javaId, packet.getEmoteId()));
                }
            }
        }
    }

    private void playEmote(GeyserSession otherSession, int javaId, String emoteId) {
        Entity otherEntity = otherSession.getEntityCache().getEntityByJavaId(javaId); // Must be ran on same thread
        if (!(otherEntity instanceof PlayerEntity otherPlayer)) return;
        otherSession.showEmote(otherPlayer, emoteId);
    }
}
