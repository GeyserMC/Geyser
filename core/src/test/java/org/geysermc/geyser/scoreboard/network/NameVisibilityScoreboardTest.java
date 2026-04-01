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

import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.translator.protocol.java.entity.JavaSetEntityDataTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetPlayerTeamTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnArmorStand;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnPlayerSilently;

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
            
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§4prefix§r§4player1§r§4suffix");
                return packet;
            });
            assertNoNextPacket(context);

            
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
                packet.getMetadata().put(EntityDataTypes.NAME, "");
                return packet;
            });
            assertNoNextPacket(context);

            
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
            
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§4prefix§r§4player1§r§4suffix");
                return packet;
            });
            assertNoNextPacket(context);

            
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

            
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket("team1", TeamAction.ADD_PLAYER, new String[]{"Tim203"})
            );
            assertNoNextPacket(context);
        });
    }

    @Test
    void teamsDontOverrideCustomName() {
        mockContextScoreboard(context -> {
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();
            var setEntityMetadataTranslator = new JavaSetEntityDataTranslator();

            spawnPlayerSilently(context, "Tim203", 2);
            spawnArmorStand(context, 3);

            
            ClientboundSetEntityDataPacket showNamePacket = new ClientboundSetEntityDataPacket(3,
                new EntityMetadata[]{
                    
                    new BooleanEntityMetadata(3, MetadataTypes.BOOLEAN, true)
                }
            );

            context.translate(setEntityMetadataTranslator, showNamePacket);

            
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(3);
                packet.getMetadata().put(EntityDataTypes.NAME, "entity.minecraft.armor_stand");
                packet.getMetadata().put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
                packet.getMetadata().put(EntityDataTypes.SCALE, 1f);

                
                packet.getMetadata().putFlags(new EnumMap<>(
                    Map.of(
                        EntityFlag.INVISIBLE, false,
                        EntityFlag.CAN_SHOW_NAME, true,
                        EntityFlag.SILENT, true,
                        EntityFlag.CAN_CLIMB, true,
                        EntityFlag.HAS_COLLISION, true,
                        EntityFlag.HAS_GRAVITY, true,
                        EntityFlag.HIDDEN_WHEN_INVISIBLE, true
                    )
                ));
                return packet;
            });

            
            ClientboundSetEntityDataPacket customNamePacket = new ClientboundSetEntityDataPacket(3,
                new EntityMetadata[]{
                    
                    new ObjectEntityMetadata<>(2, MetadataTypes.OPTIONAL_COMPONENT, Optional.of(Component.text("Custom Name")))
                }
            );

            context.translate(setEntityMetadataTranslator, customNamePacket);

            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(3);
                packet.getMetadata().put(EntityDataTypes.NAME, "Custom Name");
                packet.getMetadata().put(EntityDataTypes.SCALE, 1f);
                return packet;
            });

            
            

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
                    new String[]{"Tim203"}
                )
            );
            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(2);
                packet.getMetadata().put(EntityDataTypes.NAME, "§4prefix§r§4Tim203§r§4suffix");
                return packet;
            });

            
            assertNoNextPacket(context);
        });
    }
}
