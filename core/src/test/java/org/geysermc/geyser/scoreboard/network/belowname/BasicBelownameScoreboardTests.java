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

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketMatch;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketsMatchUnordered;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketsUnordered;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnPlayer;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnPlayerSilently;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaResetScorePacket;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetDisplayObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetScoreTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.FixedFormat;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundResetScorePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import org.junit.jupiter.api.Test;

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
    void displayWithOnePlayerAndHideObjective() {
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
    void numberFormatFixed() {
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
                    new FixedFormat(Component.text("yes", NamedTextColor.GREEN))
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
                packet.getMetadata().put(EntityDataTypes.SCORE, "§ayes §robjective");
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
    void overrideObjectiveAndRemove() {
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

    @Test
    void onlyUpdateChangedEntities() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();
            var resetScoreTranslator = new JavaResetScorePacket();

            spawnPlayerSilently(context, "player1", 2);
            spawnPlayerSilently(context, "player2", 3);

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

            // Order seems to be unstable, which imo is fine, so we account for that.
            assertNextPacketsMatchUnordered(context, SetEntityDataPacket.class, List.of(
                actual -> {
                    assertEquals(2, actual.getRuntimeEntityId());
                    if (actual.getMetadata().size() != 1 || !actual.getMetadata().containsKey(EntityDataTypes.SCORE)) {
                        throw new IllegalArgumentException("Expected (only) a name change! Received: " + actual.getMetadata());
                    }
                    assertEquals("0 §robjective", actual.getMetadata().get(EntityDataTypes.SCORE));
                },
                actual -> {
                    assertEquals(3, actual.getRuntimeEntityId());
                    if (actual.getMetadata().size() != 1 || !actual.getMetadata().containsKey(EntityDataTypes.SCORE)) {
                        throw new IllegalArgumentException("Expected (only) a name change! Received: " + actual.getMetadata());
                    }
                    assertEquals("0 §robjective", actual.getMetadata().get(EntityDataTypes.SCORE));
                }
            ));
            assertNoNextPacket(context);

            // The player's own score is not visible
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("Tim203", "objective", 1));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("player1", "objective", 1));
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "1 §robjective");
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, new ClientboundSetScorePacket("player2", "objective", 1));
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(3);
                packet.getMetadata().put(EntityDataTypes.SCORE, "1 §robjective");
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(resetScoreTranslator, new ClientboundResetScorePacket("player2", "objective"));
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(3);
                packet.getMetadata().put(EntityDataTypes.SCORE, "0 §robjective");
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(resetScoreTranslator, new ClientboundResetScorePacket("Tim203", "objective"));
            assertNoNextPacket(context);
        });
    }

    @Test
    void updateScoresWhenObjectiveNumberFormatChanges() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();

            PlayerEntity player1 = spawnPlayerSilently(context, "player1", 2);

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
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("player1", "objective1", 1));
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.BELOW_NAME, "objective1")
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "1 §robjective1");
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setScoreTranslator,
                new ClientboundSetScorePacket("player2", "objective1", 1).withNumberFormat(new FixedFormat(Component.text("abc")))
            );
            PlayerEntity player2 = spawnPlayer(context, "player2", 3);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals("abc §robjective1", packet.getMetadata().get(EntityDataTypes.SCORE));
            });

            // First check just updating the number format
            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "objective1",
                    ObjectiveAction.UPDATE,
                    Component.text("objective1"),
                    ScoreType.INTEGER,
                    new FixedFormat(Component.text("hi")))
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.SCORE, "hi §robjective1");
                return packet;
            });
            assertNoNextPacket(context);

            // Ensure state is cleanly reset for the propagated number format score, by updating a score that has its own number format
            player1.clearCachedScoreUnsafe();
            player2.clearCachedScoreUnsafe();
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("player2", "objective1", 2).withNumberFormat(new FixedFormat(Component.text("abc"))));
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(3);
                packet.getMetadata().put(EntityDataTypes.SCORE, "abc §robjective1");
                return packet;
            });
            assertNoNextPacket(context);

            // But also check updating both the objective name and format
            context.translate(
                setObjectiveTranslator,
                new ClientboundSetObjectivePacket(
                    "objective1",
                    ObjectiveAction.UPDATE,
                    Component.text("obj"),
                    ScoreType.INTEGER,
                    new FixedFormat(Component.text("hello")))
            );
            assertNextPacketsUnordered(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(3);
                packet.getMetadata().put(EntityDataTypes.SCORE, "abc §robj");

                var packet2 = new SetEntityDataPacket();
                packet2.setRuntimeEntityId(2);
                packet2.getMetadata().put(EntityDataTypes.SCORE, "hello §robj");
                return List.of(packet, packet2);
            });
            assertNoNextPacket(context);

            // Ensure state is cleanly reset for the propagated number format score, by updating a score that has its own number format
            player1.clearCachedScoreUnsafe();
            player2.clearCachedScoreUnsafe();
            context.translate(setScoreTranslator, new ClientboundSetScorePacket("player2", "objective1", 3).withNumberFormat(new FixedFormat(Component.text("abc"))));
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(3);
                packet.getMetadata().put(EntityDataTypes.SCORE, "abc §robj");
                return packet;
            });
            assertNoNextPacket(context);
        });
    }
}
