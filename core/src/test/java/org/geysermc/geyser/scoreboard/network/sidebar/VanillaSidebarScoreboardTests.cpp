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
#include "org.cloudburstmc.protocol.bedrock.data.ScoreInfo"
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

public class VanillaSidebarScoreboardTests {
    @Test
    void displayAndAddScore() {
        mockContextScoreboard(context -> {
           var setObjectiveTranslator = new JavaSetObjectiveTranslator();
           var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
           var setScoreTranslator = new JavaSetScoreTranslator();

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
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("owner", "objective", 1));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "owner")));
                return packet;
            });
        });
    }

    @Test
    void displayAndChangeScoreValue() {
        mockContextScoreboard(context -> {
           var setObjectiveTranslator = new JavaSetObjectiveTranslator();
           var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
           var setScoreTranslator = new JavaSetScoreTranslator();

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
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("owner", "objective", 1));
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
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "owner")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("owner", "objective", 2));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 2, "owner")));
                return packet;
            });
        });
    }

    @Test
    void displayAndChangeScoreDisplayName() {

        mockContextScoreboard(context -> {
           var setObjectiveTranslator = new JavaSetObjectiveTranslator();
           var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
           var setScoreTranslator = new JavaSetScoreTranslator();

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
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("owner", "objective", 1));
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
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "owner")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setScoreTranslator,
                new ClientboundSetScorePacket("owner", "objective", 1).withDisplay(Component.text("hi"))
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "hi")));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "hi")));
                return packet;
            });
        });
    }

    @Test
    void displayAndChangeScoreDisplayNameAndValue() {

        mockContextScoreboard(context -> {
           var setObjectiveTranslator = new JavaSetObjectiveTranslator();
           var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
           var setScoreTranslator = new JavaSetScoreTranslator();

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
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("owner", "objective", 1));
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
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "owner")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setScoreTranslator,
                new ClientboundSetScorePacket("owner", "objective", 2).withDisplay(Component.text("hi"))
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 2, "hi")));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 2, "hi")));
                return packet;
            });
        });
    }
}
