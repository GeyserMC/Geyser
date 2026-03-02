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
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetDisplayObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetScoreTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import org.junit.jupiter.api.Test;

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
        // this ensures that MCPE-143063 is properly handled
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
        // this ensures that MCPE-143063 is properly handled
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
