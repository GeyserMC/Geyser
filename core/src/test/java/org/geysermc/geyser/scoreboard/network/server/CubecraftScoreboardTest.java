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

package org.geysermc.geyser.scoreboard.network.server;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketMatch;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnPlayer;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetScorePacket;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetDisplayObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetPlayerTeamTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetScoreTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import org.junit.jupiter.api.Test;

public class CubecraftScoreboardTest {
    @Test
    void test() {
        mockContextScoreboard(context -> {
            var setTeamTranslator = new JavaSetPlayerTeamTranslator();
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();

            // unused
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("SB_NoName", Component.text("SB_NoName"), Component.empty(), Component.empty(), true, true, NameTagVisibility.NEVER, CollisionRule.NEVER, TeamColor.RESET, new String[0]));
            assertNoNextPacket(context);

            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "sidebar",
                    ObjectiveAction.ADD,
                    Component.text("sidebar"),
                    ScoreType.INTEGER,
                    null
                )
            );
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.SIDEBAR, "sidebar")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("sidebar");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });


            // Now they're going to create a bunch of teams and add players to those teams in a very inefficient way.
            // Presumably this is a leftover from an old system, as these don't seem to do anything but hide their nametags.
            // For which you could just use a single team.


            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.RESET, new String[0]));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), false, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), false, false, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.NEVER, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "A_Player" }));

            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.RESET, new String[0]));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), false, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), false, false, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.NEVER, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", TeamAction.ADD_PLAYER, new String[] { "B_Player" }));

            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "C_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "D_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1y|11", TeamAction.ADD_PLAYER, new String[] { "E_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "F_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "G_Player" }));

            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.RESET, new String[0]));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), false, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), false, false, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.NEVER, TeamColor.BLUE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", TeamAction.ADD_PLAYER, new String[] { "H_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "I_Player" }));

            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("22|9", Component.text("22|9"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.RESET, new String[0]));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("22|9", Component.text("22|9"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("22|9", Component.text("22|9"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("22|9", Component.text("22|9"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("22|9", Component.text("22|9"), Component.empty(), Component.empty(), false, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("22|9", Component.text("22|9"), Component.empty(), Component.empty(), false, false, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("22|9", Component.text("22|9"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("22|9", Component.text("22|9"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.NEVER, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("22|9", TeamAction.ADD_PLAYER, new String[] { "J_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "K_Player" }));

            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("26|7", Component.text("26|7"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.RESET, new String[0]));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("26|7", Component.text("26|7"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("26|7", Component.text("26|7"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("26|7", Component.text("26|7"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("26|7", Component.text("26|7"), Component.empty(), Component.empty(), false, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("26|7", Component.text("26|7"), Component.empty(), Component.empty(), false, false, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("26|7", Component.text("26|7"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("26|7", Component.text("26|7"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.NEVER, TeamColor.AQUA));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("26|7", TeamAction.ADD_PLAYER, new String[] { "L_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2e|3", TeamAction.ADD_PLAYER, new String[] { "M_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "N_Player" }));

            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.RESET, new String[0]));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), true, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), false, true, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), false, false, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), false, false, NameTagVisibility.NEVER, CollisionRule.NEVER, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("1u|13", TeamAction.ADD_PLAYER, new String[] { "O_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "P_Player" }));
            context.translate(setTeamTranslator, new ClientboundSetPlayerTeamPacket("2i|1", TeamAction.ADD_PLAYER, new String[] { "Q_Player" }));

            assertNoNextPacket(context);


            // Now that those teams are created and people added to it, they set the final sidebar name and add the lines to it.
            // They're also not doing this efficiently, because they don't add the players when the team is created.
            // Instead, they send an additional packet.


            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "sidebar",
                    ObjectiveAction.UPDATE,
                    Component.empty()
                        .append(Component.text(
                            "CubeCraft", NamedTextColor.WHITE, TextDecoration.BOLD)),
                    ScoreType.INTEGER,
                    null));
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("0");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("§f§lCubeCraft");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-0",
                    Component.text("SB_l-0"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-0", TeamAction.ADD_PLAYER, new String[] {"§0§0"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-0",
                    Component.text("SB_l-0"),
                    Component.empty().append(Component.text("", NamedTextColor.BLACK)),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§0", "sidebar", 10));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 10, "§r§0§0§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-1",
                    Component.text("SB_l-1"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-1", TeamAction.ADD_PLAYER, new String[] {"§0§1"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-1",
                    Component.text("SB_l-1"),
                    Component.empty()
                        .append(Component.textOfChildren(
                            Component.text("User: ", TextColor.color(0x3aa9ff)),
                            Component.text("Tim203", NamedTextColor.WHITE))),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§1", "sidebar", 9));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(2, "0", 9, "§bUser: §r§fTim203§r§0§1§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-2",
                    Component.text("SB_l-2"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-2", TeamAction.ADD_PLAYER, new String[] {"§0§2"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-2",
                    Component.text("SB_l-2"),
                    Component.empty()
                        .append(Component.textOfChildren(
                            Component.text("Rank: ", TextColor.color(0x3aa9ff)),
                            Component.text("\uE1AB ", NamedTextColor.WHITE))),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§2", "sidebar", 8));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(3, "0", 8, "§bRank: §r§f\uE1AB §r§0§2§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-3",
                    Component.text("SB_l-3"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-3", TeamAction.ADD_PLAYER, new String[] {"§0§3"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-3",
                    Component.text("SB_l-3"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§3", "sidebar", 7));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(4, "0", 7, "§r§0§3§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-4",
                    Component.text("SB_l-4"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-4", TeamAction.ADD_PLAYER, new String[] {"§0§4"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-4",
                    Component.text("SB_l-4"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§4", "sidebar", 6));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(5, "0", 6, "§r§0§4§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-5",
                    Component.text("SB_l-5"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-5", TeamAction.ADD_PLAYER, new String[] {"§0§5"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-5",
                    Component.text("SB_l-5"),
                    Component.empty().append(Component.text("", NamedTextColor.DARK_BLUE)),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§5", "sidebar", 5));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(6, "0", 5, "§r§0§5§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-6",
                    Component.text("SB_l-6"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-6", TeamAction.ADD_PLAYER, new String[] {"§0§6"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-6",
                    Component.text("SB_l-6"),
                    Component.empty()
                        .append(Component.textOfChildren(
                            Component.text("Lobby: ", TextColor.color(0x3aa9ff)),
                            Component.text("EU #10", NamedTextColor.WHITE))),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§6", "sidebar", 4));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(7, "0", 4, "§bLobby: §r§fEU #10§r§0§6§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-7",
                    Component.text("SB_l-7"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-7", TeamAction.ADD_PLAYER, new String[] {"§0§7"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-7",
                    Component.text("SB_l-7"),
                    Component.empty()
                        .append(Component.textOfChildren(
                            Component.text("Players: ", TextColor.color(0x3aa9ff)),
                            Component.text("783", NamedTextColor.WHITE))),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§7", "sidebar", 3));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(8, "0", 3, "§bPlayers: §r§f783§r§0§7§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-8",
                    Component.text("SB_l-8"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-8", TeamAction.ADD_PLAYER, new String[] {"§0§8"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-8",
                    Component.text("SB_l-8"),
                    Component.empty().append(Component.text("", NamedTextColor.DARK_GREEN)),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§8", "sidebar", 2));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(9, "0", 2, "§r§0§8§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-9",
                    Component.text("SB_l-9"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-9", TeamAction.ADD_PLAYER, new String[] {"§0§9"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-9",
                    Component.text("SB_l-9"),
                    Component.empty().append(Component.text("24/09/24 (g2208)", TextColor.color(0x777777))),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§9", "sidebar", 1));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(10, "0", 1, "§824/09/24 (g2208)§r§0§9§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-10",
                    Component.text("SB_l-10"),
                    Component.empty(),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET,
                    new String[0]));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket("SB_l-10", TeamAction.ADD_PLAYER, new String[] {"§0§a"}));
            context.translate(
                setTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "SB_l-10",
                    Component.text("SB_l-10"),
                    Component.empty().append(Component.text("play.cubecraft.net", NamedTextColor.GOLD)),
                    Component.empty(),
                    true,
                    true,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.ALWAYS,
                    TeamColor.RESET));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("§0§a", "sidebar", 0));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(11, "0", 0, "§6play.cubecraft.net§r§0§a§r")));
                return packet;
            });

            // after this we get a ClientboundPlayerInfoUpdatePacket with the action UPDATE_DISPLAY_NAME,
            // but that one is only shown in the tablist so we don't have to handle that.
            // And after that we get each player's ClientboundPlayerInfoUpdatePacket with also a UPDATE_DISPLAY_NAME,
            // which is also not interesting for us.
            // CubeCraft seems to use two armor stands per player: 1 for the rank badge and 1 for the player name.
            // So the only thing we have to verify is that the nametag is hidden

            spawnPlayer(context, "A_Player", 2);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(2, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "B_Player", 3);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(3, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "E_Player", 4);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(4, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "H_Player", 5);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(5, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "J_Player", 6);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(6, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "K_Player", 7);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(7, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "L_Player", 8);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(8, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "O_Player", 9);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(9, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });
        });
    }
}
