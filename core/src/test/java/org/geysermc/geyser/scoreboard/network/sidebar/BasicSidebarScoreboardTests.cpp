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

#include "static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket"
#include "static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket"
#include "static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard"

#include "java.util.List"
#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.format.NamedTextColor"
#include "net.kyori.adventure.text.format.TextDecoration"
#include "org.cloudburstmc.protocol.bedrock.data.ScoreInfo"
#include "org.cloudburstmc.protocol.bedrock.packet.RemoveObjectivePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetScorePacket"
#include "org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetDisplayObjectiveTranslator"
#include "org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetObjectiveTranslator"
#include "org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetScoreTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket"
#include "org.junit.jupiter.api.Test"

/*
Identical to playerlist
 */
public class BasicSidebarScoreboardTests {
    @Test
    void displayAndRemove() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();

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
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.PLAYER_LIST, "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("objective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("list");
                packet.setSortOrder(1);
                return packet;
            });

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.PLAYER_LIST, "")
            );
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("0");
                return packet;
            });
        });
    }

    @Test
    void displayNameColors() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();

            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "objective",
                    ObjectiveAction.ADD,
                    Component.text("objective", NamedTextColor.AQUA, TextDecoration.BOLD),
                    ScoreType.INTEGER,
                    null
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
                packet.setDisplayName("§b§lobjective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
        });
    }

    @Test
    void override() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();

            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "objective1",
                    ObjectiveAction.ADD,
                    Component.text("objective1"),
                    ScoreType.INTEGER,
                    null
                )
            );

            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "objective2",
                    ObjectiveAction.ADD,
                    Component.text("objective2"),
                    ScoreType.INTEGER,
                    null
                )
            );

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("Tim203", "objective1", 1));
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("Tim203", "objective2", 2)); 
            assertNoNextPacket(context);


            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.SIDEBAR, "objective2")
            );

            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("objective2");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 2, "Tim203")));
                return packet;
            });
            assertNoNextPacket(context);


            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.SIDEBAR, "objective1")
            );

            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("0");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("2");
                packet.setDisplayName("objective1");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(3, "2", 1, "Tim203")));
                return packet;
            });
        });
    }
}
