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

package org.geysermc.geyser.scoreboard.network.sidebar;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetScorePacket;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaResetScorePacket;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetDisplayObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetPlayerTeamTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetScoreTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundResetScorePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import org.junit.jupiter.api.Test;

public class OrderAndLimitSidebarScoreboardTests {
    @Test
    void aboveDisplayLimit() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();
            var resetScoreTranslator = new JavaResetScorePacket();

            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "objective",
                    ObjectiveAction.ADD,
                    Component.text("objective"),
                    ScoreType.INTEGER,
                    null
                )
            );

            // some are in an odd order to make sure that there is no bias for which score is send first,
            // and to make sure that the score value also doesn't influence the order
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("a", "objective", 1));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("b", "objective", 2));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("c", "objective", 3));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("d", "objective", 5));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("e", "objective", 4));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("f", "objective", 6));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("g", "objective", 9));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("h", "objective", 8));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("i", "objective", 7));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("p", "objective", 10));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("o", "objective", 11));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("n", "objective", 12));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("m", "objective", 13));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("k", "objective", 14));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("l", "objective", 15));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("j", "objective", 16));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("q", "objective", 17));
            assertNoNextPacket(context);


            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.SIDEBAR, "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("objective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(1, "0", 17, "q"),
                    new ScoreInfo(2, "0", 16, "j"),
                    new ScoreInfo(3, "0", 15, "l"),
                    new ScoreInfo(4, "0", 14, "k"),
                    new ScoreInfo(5, "0", 13, "m"),
                    new ScoreInfo(6, "0", 12, "n"),
                    new ScoreInfo(7, "0", 11, "o"),
                    new ScoreInfo(8, "0", 10, "p"),
                    new ScoreInfo(9, "0", 9, "g"),
                    new ScoreInfo(10, "0", 8, "h"),
                    new ScoreInfo(11, "0", 7, "i"),
                    new ScoreInfo(12, "0", 6, "f"),
                    new ScoreInfo(13, "0", 5, "d"),
                    new ScoreInfo(14, "0", 4, "e"),
                    new ScoreInfo(15, "0", 3, "c")
                ));
                return packet;
            });
            assertNoNextPacket(context);

            // remove a score
            context.translate(
                resetScoreTranslator,
                new ClientboundResetScorePacket("m", "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(5, "0", 13, "m")));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(16, "0", 2, "b")));
                return packet;
            });

            // add a score
            context.translate(
                setScoreTranslator,
                new ClientboundSetScorePacket("aa", "objective", 13)
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(16, "0", 2, "b")));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(17, "0", 13, "aa")));
                return packet;
            });

            // add score with same score value (after)
            context.translate(
                setScoreTranslator,
                new ClientboundSetScorePacket("ga", "objective", 9)
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(
                    new ScoreInfo(15, "0", 3, "c"),
                    new ScoreInfo(9, "0", 9, "§0§rg")
                ));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(9, "0", 9, "§0§rg"),
                    new ScoreInfo(18, "0", 9, "§1§rga")
                ));
                return packet;
            });

            // add another score with same score value (before all)
            context.translate(
                setScoreTranslator,
                new ClientboundSetScorePacket("ag", "objective", 9)
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(
                    new ScoreInfo(14, "0", 4, "e"),
                    new ScoreInfo(9, "0", 9, "§1§rg"),
                    new ScoreInfo(18, "0", 9, "§2§rga")
                ));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(19, "0", 9, "§0§rag"),
                    new ScoreInfo(9, "0", 9, "§1§rg"),
                    new ScoreInfo(18, "0", 9, "§2§rga")
                ));
                return packet;
            });

            // remove score with same value
            context.translate(
                resetScoreTranslator,
                new ClientboundResetScorePacket("g", "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(
                    new ScoreInfo(9, "0", 9, "§1§rg"),
                    new ScoreInfo(18, "0", 9, "§1§rga")
                ));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(18, "0", 9, "§1§rga"),
                    new ScoreInfo(20, "0", 4, "e")
                ));
                return packet;
            });

            // remove the other score with the same value
            context.translate(
                resetScoreTranslator,
                new ClientboundResetScorePacket("ga", "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(
                    new ScoreInfo(18, "0", 9, "§1§rga"),
                    new ScoreInfo(19, "0", 9, "ag")
                ));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(19, "0", 9, "ag"),
                    new ScoreInfo(21, "0", 3, "c")
                ));
                return packet;
            });
        });
    }

    @Test
    void aboveDisplayLimitWithTeam() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();
            var resetScoreTranslator = new JavaResetScorePacket();
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();

            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "objective",
                    ObjectiveAction.ADD,
                    Component.text("objective"),
                    ScoreType.INTEGER,
                    null
                )
            );

            // some are in an odd order to make sure that there is no bias for which score is send first,
            // and to make sure that the score value also doesn't influence the order
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("a", "objective", 1));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("b", "objective", 2));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("c", "objective", 3));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("d", "objective", 5));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("e", "objective", 4));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("f", "objective", 6));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("g", "objective", 9));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("h", "objective", 8));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("i", "objective", 7));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("p", "objective", 10));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("o", "objective", 11));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("n", "objective", 12));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("m", "objective", 13));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("k", "objective", 14));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("l", "objective", 15));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("j", "objective", 16));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("q", "objective", 17));
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
                    new String[]{ "f", "o" }
                )
            );
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.SIDEBAR, "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("objective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(1, "0", 17, "q"),
                    new ScoreInfo(2, "0", 16, "j"),
                    new ScoreInfo(3, "0", 15, "l"),
                    new ScoreInfo(4, "0", 14, "k"),
                    new ScoreInfo(5, "0", 13, "m"),
                    new ScoreInfo(6, "0", 12, "n"),
                    new ScoreInfo(7, "0", 11, "§4prefix§r§4o§r§4suffix"),
                    new ScoreInfo(8, "0", 10, "p"),
                    new ScoreInfo(9, "0", 9, "g"),
                    new ScoreInfo(10, "0", 8, "h"),
                    new ScoreInfo(11, "0", 7, "i"),
                    new ScoreInfo(12, "0", 6, "§4prefix§r§4f§r§4suffix"),
                    new ScoreInfo(13, "0", 5, "d"),
                    new ScoreInfo(14, "0", 4, "e"),
                    new ScoreInfo(15, "0", 3, "c")
                ));
                return packet;
            });
            assertNoNextPacket(context);

            // remove a score
            context.translate(
                resetScoreTranslator,
                new ClientboundResetScorePacket("m", "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(5, "0", 13, "m")));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(16, "0", 2, "b")));
                return packet;
            });

            // add a score
            context.translate(
                setScoreTranslator,
                new ClientboundSetScorePacket("aa", "objective", 13)
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(16, "0", 2, "b")));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(17, "0", 13, "aa")));
                return packet;
            });

            // add some teams for the upcoming score adds
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
                    TeamColor.DARK_AQUA,
                    new String[]{ "oa" }
                )
            );
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "team3",
                    Component.text("displayName"),
                    Component.text("prefix"),
                    Component.text("suffix"),
                    false,
                    false,
                    NameTagVisibility.ALWAYS,
                    CollisionRule.NEVER,
                    TeamColor.DARK_PURPLE,
                    new String[]{ "ao" }
                )
            );
            assertNoNextPacket(context);

            // add a score that on Java should be after 'o', but would be before on Bedrock without manual order
            // due to the team color
            context.translate(
                setScoreTranslator,
                new ClientboundSetScorePacket("oa", "objective", 11)
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(
                    new ScoreInfo(15, "0", 3, "c"),
                    new ScoreInfo(7, "0", 11, "§0§r§4prefix§r§4o§r§4suffix")
                ));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(7, "0", 11, "§0§r§4prefix§r§4o§r§4suffix"),
                    new ScoreInfo(18, "0", 11, "§1§r§3prefix§r§3oa§r§3suffix")
                ));
                return packet;
            });

            // add a score that on Java should be before 'o', but would be after on Bedrock without manual order
            // due to the team color
            context.translate(
                setScoreTranslator,
                new ClientboundSetScorePacket("ao", "objective", 11)
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(
                    new ScoreInfo(14, "0", 4, "e"),
                    new ScoreInfo(7, "0", 11, "§1§r§4prefix§r§4o§r§4suffix"),
                    new ScoreInfo(18, "0", 11, "§2§r§3prefix§r§3oa§r§3suffix")
                ));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(19, "0", 11, "§0§r§5prefix§r§5ao§r§5suffix"),
                    new ScoreInfo(7, "0", 11, "§1§r§4prefix§r§4o§r§4suffix"),
                    new ScoreInfo(18, "0", 11, "§2§r§3prefix§r§3oa§r§3suffix")
                ));
                return packet;
            });

            // remove original 'o' score
            context.translate(
                resetScoreTranslator,
                new ClientboundResetScorePacket("o", "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(
                    new ScoreInfo(7, "0", 11, "§1§r§4prefix§r§4o§r§4suffix"),
                    new ScoreInfo(18, "0", 11, "§1§r§3prefix§r§3oa§r§3suffix")
                ));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(18, "0", 11, "§1§r§3prefix§r§3oa§r§3suffix"),
                    new ScoreInfo(20, "0", 4, "e")
                ));
                return packet;
            });

            // remove the other score with the same value as 'o'
            context.translate(
                resetScoreTranslator,
                new ClientboundResetScorePacket("oa", "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(
                    new ScoreInfo(18, "0", 11, "§1§r§3prefix§r§3oa§r§3suffix"),
                    new ScoreInfo(19, "0", 11, "§5prefix§r§5ao§r§5suffix")
                ));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(19, "0", 11, "§5prefix§r§5ao§r§5suffix"),
                    new ScoreInfo(21, "0", 3, "c")
                ));
                return packet;
            });
        });
    }
}
