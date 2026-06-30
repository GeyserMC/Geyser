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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import org.cloudburstmc.protocol.bedrock.data.EmoteFlag;
import org.cloudburstmc.protocol.bedrock.packet.EmotePacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes;
import org.geysermc.geyser.api.event.bedrock.ClientEmoteEvent;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.farm.PigEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import java.util.UUID;

@Translator(packet = EmotePacket.class)
public class BedrockEmoteTranslator extends PacketTranslator<EmotePacket> {

    @Override
    public void translate(GeyserSession session, EmotePacket packet) {
        // For the future: could have a method that exposes which players will see the emote
        ClientEmoteEvent event = new ClientEmoteEvent(session, packet.getEmoteId());
        if (!GeyserImpl.getInstance().config().gameplay().emotesEnabled()) {
            event.setCancelled(true);
        }

        // hack of course
        var player = session.getPlayerEntity();
        UUID uuid = UUID.fromString(packet.getEmoteId());
        int i = session.getEmotes().indexOf(uuid);
        switch (i) {
            case 0:
                player.update(GeyserEntityDataTypes.ROTATE_RIDER_DEGREES, player.value(GeyserEntityDataTypes.ROTATE_RIDER_DEGREES) == null ? 180f : null);
                break;
            case 1:
                player.update(GeyserEntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, player.value(GeyserEntityDataTypes.SEAT_HAS_ROTATION) == null ? 360f : null);
                break;
            case 2:
                player.update(GeyserEntityDataTypes.SEAT_HAS_ROTATION, player.value(GeyserEntityDataTypes.SEAT_HAS_ROTATION) == null ? true : false);
                break;
            case 3:
                player.update(GeyserEntityDataTypes.ROTATION_LOCKED_TO_VEHICLE, player.value(GeyserEntityDataTypes.ROTATION_LOCKED_TO_VEHICLE) == null ? true : false);
                break;
            default:
                System.out.println("Invalid emote id: " + i);
        }

        var entity = session.getEntityCache().getEntities().values().stream().filter(e -> e instanceof PigEntity).findFirst().orElseThrow();
        switch (i) {
            case 0:
                entity.update(GeyserEntityDataTypes.ROTATE_RIDER_DEGREES, player.value(GeyserEntityDataTypes.ROTATE_RIDER_DEGREES) == null ? 180f : null);
                break;
            case 1:
                entity.update(GeyserEntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, player.value(GeyserEntityDataTypes.SEAT_HAS_ROTATION) == null ? 360f : null);
                break;
            case 2:
                entity.update(GeyserEntityDataTypes.SEAT_HAS_ROTATION, player.value(GeyserEntityDataTypes.SEAT_HAS_ROTATION) == null ? true : false);
                break;
            case 3:
                entity.update(GeyserEntityDataTypes.ROTATION_LOCKED_TO_VEHICLE, player.value(GeyserEntityDataTypes.ROTATION_LOCKED_TO_VEHICLE) == null ? true : false);
                break;
            default:
                System.out.println("Invalid emote id: " + i);
        }

        session.getGeyser().eventBus().fire(event);
        if (event.isCancelled()) {
            return;
        }

        int javaId = session.getPlayerEntity().getEntityId();
        String xuid = session.getAuthData().xuid();
        String emote = packet.getEmoteId();
        for (GeyserSession otherSession : session.getGeyser().getSessionManager().getSessions().values()) {
            if (otherSession != session) {
                if (otherSession.isClosed()) continue;

                otherSession.ensureInEventLoop(() -> playEmote(otherSession, javaId, xuid, emote, event.silent()));
            }
        }
    }

    /**
     * Play an emote by an emoter to the given session.
     * This method must be called within the session's event loop.
     *
     * @param session the session to show the emote to
     * @param emoterJavaId the java id of the emoter
     * @param emoterXuid the xuid of the emoter
     * @param emoteId the emote to play
     * @param silent whether to suppress the emote chat announcement
     */
    private static void playEmote(GeyserSession session, int emoterJavaId, String emoterXuid, String emoteId, boolean silent) {
        Entity emoter = session.getEntityCache().getEntityByJavaId(emoterJavaId); // Must be ran on same thread
        if (emoter instanceof PlayerEntity) {
            EmotePacket packet = new EmotePacket();
            packet.setRuntimeEntityId(emoter.geyserId());
            packet.setXuid(emoterXuid);
            packet.setPlatformId(""); // BDS sends empty
            packet.setEmoteId(emoteId);
            packet.getFlags().add(EmoteFlag.SERVER_SIDE);
            if (silent) {
                packet.getFlags().add(EmoteFlag.MUTE_EMOTE_CHAT);
            }
            session.sendUpstreamPacket(packet);
        }
    }
}
