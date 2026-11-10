/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnPlayerSilently;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetPlayerTeamTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.junit.jupiter.api.Test;

public class NametagScoreboardTests {
    @Test
    void teamAddEntityTestOverrides() {
        mockContextScoreboard(context -> {
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();

            spawnPlayerSilently(context, "player1", 2);

            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team1",
                    Component.text("", NamedTextColor.RED),
                    Component.text("", NamedTextColor.GREEN),
                    Component.text("", NamedTextColor.BLUE),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.WHITE,
                    new String[] {"player1"}
                )
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§f§r§fplayer1§r§f");
                return packet;
            });
            assertNoNextPacket(context);

            // Ensure that the entity is tracking updates on team1
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team1",
                    Component.text("", NamedTextColor.RED),
                    Component.text("hi", NamedTextColor.GREEN),
                    Component.text("", NamedTextColor.BLUE),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.WHITE
                )
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§f§ahi§r§fplayer1§r§f");
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team2",
                    Component.text("", NamedTextColor.BLUE),
                    Component.text("", NamedTextColor.GREEN),
                    Component.text("", NamedTextColor.RED),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.BLACK,
                    new String[] {"player1"}
                )
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§0§r§0player1§r§0");
                return packet;
            });
            assertNoNextPacket(context);


            // Ensure that the entity is no longer tracking updates from team1
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team1",
                    Component.text("", NamedTextColor.RED),
                    Component.text("hello", NamedTextColor.GREEN),
                    Component.text("", NamedTextColor.BLUE),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.WHITE
                )
            );
            assertNoNextPacket(context);

            // Ensure that the entity is tracking updates from team2
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team2",
                    Component.text("", NamedTextColor.BLUE),
                    Component.text("hi", NamedTextColor.GREEN),
                    Component.text("", NamedTextColor.RED),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.BLACK
                )
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§0§ahi§r§0player1§r§0");
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket("team1", TeamAction.ADD_PLAYER, new String[] {"player1"})
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§f§ahello§r§fplayer1§r§f");
                return packet;
            });
            assertNoNextPacket(context);

            // Ensure that the entity is no longer tracking updates from team2
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team2",
                    Component.text("", NamedTextColor.BLUE),
                    Component.text("hello", NamedTextColor.GREEN),
                    Component.text("", NamedTextColor.RED),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.BLACK
                )
            );
            assertNoNextPacket(context);

            // Ensure that the entity is tracking updates from team1
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team1",
                    Component.text("", NamedTextColor.RED),
                    Component.text("hi", NamedTextColor.GREEN),
                    Component.text("hi", NamedTextColor.BLUE),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.WHITE
                )
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§f§ahi§r§fplayer1§r§f§9hi");
                return packet;
            });
            assertNoNextPacket(context);
        });
    }
}
