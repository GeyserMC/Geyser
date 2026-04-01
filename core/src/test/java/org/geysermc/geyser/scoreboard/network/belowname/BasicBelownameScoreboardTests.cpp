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

package org.geysermc.geyser.scoreboard.network.belowname;

#include "static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket"
#include "static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket"
#include "static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnPlayerSilently"
#include "static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard"

#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.format.NamedTextColor"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket"
#include "org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetDisplayObjectiveTranslator"
#include "org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetObjectiveTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket"
#include "org.junit.jupiter.api.Test"

public class BasicBelownameScoreboardTests {
    @Test
    void displayWithNoPlayersAndRemove() {
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

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.BELOW_NAME, "objective")
            );

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.BELOW_NAME, "")
            );
            assertNoNextPacket(context);
        });
    }

    @Test
    void displayColorWithOnePlayer() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();

            spawnPlayerSilently(context, "player1", 2);

            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "objective",
                    ObjectiveAction.ADD,
                    Component.text("objective", NamedTextColor.BLUE),
                    ScoreType.INTEGER,
                    null
                )
            );
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.BELOW_NAME, "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "0 §r§9objective");
                return packet;
            });
        });
    }

    @Test
    void displayWithOnePlayerAndRemove() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();

            spawnPlayerSilently(context, "player1", 2);

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
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.BELOW_NAME, "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "0 §robjective");
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.BELOW_NAME, "")
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "");
                return packet;
            });
        });
    }

    @Test
    void overrideAndRemove() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();

            spawnPlayerSilently(context, "player1", 2);

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
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.BELOW_NAME, "objective2")
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "0 §robjective2");
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.BELOW_NAME, "objective1")
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "0 §robjective1");
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.BELOW_NAME, "")
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "");
                return packet;
            });
        });
    }
}
