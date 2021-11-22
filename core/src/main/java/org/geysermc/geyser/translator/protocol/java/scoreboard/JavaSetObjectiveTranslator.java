/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.java.scoreboard;

import com.github.steveice10.mc.protocol.data.game.scoreboard.ObjectiveAction;
import com.github.steveice10.mc.protocol.data.game.scoreboard.ScoreboardPosition;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.WorldCache;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater;
import org.geysermc.geyser.scoreboard.UpdateType;

@Translator(packet = ClientboundSetObjectivePacket.class)
public class JavaSetObjectiveTranslator extends PacketTranslator<ClientboundSetObjectivePacket> {
    private final GeyserLogger logger = GeyserImpl.getInstance().getLogger();

    @Override
    public void translate(GeyserSession session, ClientboundSetObjectivePacket packet) {
        WorldCache worldCache = session.getWorldCache();
        Scoreboard scoreboard = worldCache.getScoreboard();
        int pps = worldCache.increaseAndGetScoreboardPacketsPerSecond();

        Objective objective = scoreboard.getObjective(packet.getName());
        if (objective != null && objective.getUpdateType() != UpdateType.REMOVE && packet.getAction() == ObjectiveAction.ADD) {
            // matches vanilla behaviour
            logger.warning("An objective with the same name '" + packet.getName() + "' already exists! Ignoring packet");
            return;
        }

        if ((objective == null || objective.getUpdateType() == UpdateType.REMOVE) && packet.getAction() != ObjectiveAction.REMOVE) {
            objective = scoreboard.registerNewObjective(packet.getName());
        }

        switch (packet.getAction()) {
            case ADD, UPDATE -> objective.setDisplayName(MessageTranslator.convertMessage(packet.getDisplayName()))
                    .setType(packet.getType().ordinal());
            case REMOVE -> {
                scoreboard.unregisterObjective(packet.getName());
                if (objective != null && objective == scoreboard.getObjectiveSlots().get(ScoreboardPosition.BELOW_NAME)) {
                    // Clear the score tag from all players
                    for (PlayerEntity entity : session.getEntityCache().getAllPlayerEntities()) {
                        // Other places we check for the entity being valid,
                        // but we must set the below name text as null for all players
                        // or else PlayerEntity#spawnEntity will find a null objective and not touch EntityData#SCORE_TAG
                        entity.setBelowNameText(null);
                    }
                }
            }
        }

        if (objective == null || !objective.isActive()) {
            return;
        }

        // ScoreboardUpdater will handle it for us if the packets per second
        // (for score and team packets) is higher than the first threshold
        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }
}
