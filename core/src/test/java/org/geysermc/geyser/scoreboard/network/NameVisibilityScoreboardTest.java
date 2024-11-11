/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.scoreboard.network;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnPlayerSilently;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetPlayerTeamTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.junit.jupiter.api.Test;

public class NameVisibilityScoreboardTest {
    @Test
    void playerVisibilityNever() {
        mockContextScoreboard(context -> {
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();

            spawnPlayerSilently(context, "player1", 2);

            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team1",
                    Component.text("displayName"),
                    Component.text("prefix"),
                    Component.text("suffix"),
                    false,
                    false,
                    NameTagVisibility.NEVER,
                    CollisionRule.NEVER,
                    TeamColor.DARK_RED,
                    new String[]{"player1"}
                )
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "");
                return packet;
            });
        });
    }

    @Test
    void playerVisibilityHideForOtherTeam() {
        mockContextScoreboard(context -> {
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();

            spawnPlayerSilently(context, "player1", 2);

            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team1",
                    Component.text("displayName"),
                    Component.text("prefix"),
                    Component.text("suffix"),
                    false,
                    false,
                    NameTagVisibility.HIDE_FOR_OTHER_TEAMS,
                    CollisionRule.NEVER,
                    TeamColor.DARK_RED,
                    new String[]{"player1"}
                )
            );
            // only hidden if session player (Tim203) is in a team as well
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§4prefix§r§4player1§r§4suffix");
                return packet;
            });
            assertNoNextPacket(context);

            // create another team and add Tim203 to it
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team2",
                    Component.text("displayName"),
                    Component.text("prefix"),
                    Component.text("suffix"),
                    false,
                    false,
                    NameTagVisibility.NEVER,
                    CollisionRule.NEVER,
                    TeamColor.DARK_RED,
                    new String[]{"Tim203"}
                )
            );
            // Tim203 is now in another team, so it should be hidden
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "");
                return packet;
            });
            assertNoNextPacket(context);

            // add Tim203 to same team as player1, score should be visible again
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket("team1", TeamAction.ADD_PLAYER, new String[]{"Tim203"})
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§4prefix§r§4player1§r§4suffix");
                return packet;
            });
        });
    }

    @Test
    void playerVisibilityHideForOwnTeam() {
        mockContextScoreboard(context -> {
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();

            spawnPlayerSilently(context, "player1", 2);

            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team1",
                    Component.text("displayName"),
                    Component.text("prefix"),
                    Component.text("suffix"),
                    false,
                    false,
                    NameTagVisibility.HIDE_FOR_OWN_TEAM,
                    CollisionRule.NEVER,
                    TeamColor.DARK_RED,
                    new String[]{"player1"}
                )
            );
            // Tim203 is not in a team (let alone the same team), so should be visible
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§4prefix§r§4player1§r§4suffix");
                return packet;
            });
            assertNoNextPacket(context);

            // Tim203 is now in the same team as player1, so should be hidden
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket("team1", TeamAction.ADD_PLAYER, new String[]{"Tim203"})
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "");
                return packet;
            });
            assertNoNextPacket(context);

            // create another team and add Tim203 to there, score should be visible again
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team2",
                    Component.text("displayName"),
                    Component.text("prefix"),
                    Component.text("suffix"),
                    false,
                    false,
                    NameTagVisibility.NEVER,
                    CollisionRule.NEVER,
                    TeamColor.DARK_RED,
                    new String[]{"Tim203"}
                )
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§4prefix§r§4player1§r§4suffix");
                return packet;
            });
        });
    }

    @Test
    void playerVisibilityAlways() {
        mockContextScoreboard(context -> {
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();

            spawnPlayerSilently(context, "player1", 2);

            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team1",
                    Component.text("displayName"),
                    Component.text("prefix"),
                    Component.text("suffix"),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.DARK_RED,
                    new String[]{"player1"}
                )
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§4prefix§r§4player1§r§4suffix");
                return packet;
            });

            // adding self to another team shouldn't make a difference
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team2",
                    Component.text("displayName"),
                    Component.text("prefix"),
                    Component.text("suffix"),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.DARK_RED,
                    new String[]{"Tim203"}
                )
            );
            assertNoNextPacket(context);

            // adding self to player1 team shouldn't matter
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket("team1", TeamAction.ADD_PLAYER, new String[]{"Tim203"})
            );
            assertNoNextPacket(context);
        });
    }
}
