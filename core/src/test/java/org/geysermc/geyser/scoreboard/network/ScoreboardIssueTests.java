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
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.entity.type.living.monster.EnderDragonPartEntity;
import org.geysermc.geyser.translator.protocol.java.entity.JavaAddEntityTranslator;
import org.geysermc.geyser.translator.protocol.java.entity.JavaSetEntityDataTranslator;
import org.geysermc.geyser.translator.protocol.java.entity.player.JavaPlayerInfoUpdateTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetPlayerTeamTranslator;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketMatch;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketType;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for issues reported on GitHub.
 */
public class ScoreboardIssueTests {

    /**
     * Test for <a href="https://github.com/GeyserMC/Geyser/issues/5078">#5078</a>
     */
    @Test
    void entityWithoutType() {
        // dragon entity parts are an entity in Geyser, but do not have an entity type
        mockContextScoreboard(context -> {
            // EntityUtils#translatedEntityName used to not take null EntityType's into account,
            // so it used to throw an exception
            assertDoesNotThrow(() -> {
                // dragon entity parts are not spawned using a packet, so we manually create an instance
                var dragonHeadPart = new EnderDragonPartEntity(context.session(), 2, 2, 1, 1);

                String displayName = dragonHeadPart.getDisplayName();
                assertEquals("entity.unregistered_sadface", displayName);
            });
        });
    }

    /**
     * Test for <a href="https://github.com/GeyserMC/Geyser/issues/5089">#5089</a>.
     * It follows the reproduction steps with all the packets it sends along its way.
     * Tested with the 2.0.0-SNAPSHOT version.
     * Note that this exact issue is actually 2 issues:
     * <ul>
     * <li>
     *     An issue caused by remainders of code that was part of the initial PR that added support for players.
     *     The code is now more streamlined.
     * </li>
     * <li>Armor stands are excluded from team visibility checks (the only living entity)</li>
     * </ul>
     */
    @Test
    void nameNotUpdating() {
        mockContextScoreboard(context -> {
            var playerInfoUpdateTranslator = new JavaPlayerInfoUpdateTranslator();
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();
            var addEntityTranslator = new JavaAddEntityTranslator();
            var setEntityDataTranslator = new JavaSetEntityDataTranslator();


            // first command, create NPC


            var npcUuid = UUID.fromString("b0eb01d7-52c9-4730-9fd3-2c03fcb00d6e");
            context.translate(
                playerInfoUpdateTranslator,
                new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(PlayerListEntryAction.ADD_PLAYER, PlayerListEntryAction.UPDATE_LISTED),
                    new PlayerListEntry[] {
                        new PlayerListEntry(npcUuid, new GameProfile(npcUuid, "1297"), false, 0, GameMode.SURVIVAL, null, false, 0, null, 0, null, null)
                    }));
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "npc_team_1297",
                    Component.empty(),
                    Component.empty(),
                    Component.empty(),
                    false,
                    false,
                    NameTagVisibility.NEVER,
                    CollisionRule.NEVER,
                    TeamColor.WHITE,
                    new String[0]
                )
            );
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket("npc_team_1297", TeamAction.ADD_PLAYER, new String[]{ "1297" }));

            context.translate(addEntityTranslator, new ClientboundAddEntityPacket(1297, npcUuid, EntityType.PLAYER, 1, 2, 3, 4, 5, 6));
            // then it updates the displayed skin parts, which isn't relevant for us

            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(3, packet.getRuntimeEntityId());
                assertEquals(npcUuid, packet.getUuid());
                assertEquals("1297", packet.getUsername());
                assertEquals((byte) 1, packet.getMetadata().get(EntityDataTypes.NAMETAG_ALWAYS_SHOW));
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });
            assertNoNextPacket(context);


            // second command, create hologram


            var hologramUuid = UUID.fromString("b1586291-5f68-44dc-847d-6c123c5b8cbf");
            context.translate(
                addEntityTranslator,
                new ClientboundAddEntityPacket(1298, hologramUuid, EntityType.ARMOR_STAND, 6, 5, 4, 3, 2, 1));

            assertNextPacketMatch(context, AddEntityPacket.class, packet -> {
                assertEquals(4, packet.getRuntimeEntityId());
                assertEquals("minecraft:armor_stand", packet.getIdentifier());
            });

            // metadata set: invisible, custom name, custom name visible
            context.translate(setEntityDataTranslator, new ClientboundSetEntityDataPacket(1298, new EntityMetadata<?, ?>[]{
                new ByteEntityMetadata(0, MetadataTypes.BYTE, (byte) 0x20),
                new ObjectEntityMetadata<>(2, MetadataTypes.OPTIONAL_COMPONENT, Optional.of(Component.text("tesss"))),
                new BooleanEntityMetadata(3, MetadataTypes.BOOLEAN, true)
            }));

            assertNextPacketMatch(context, SetEntityDataPacket.class, packet -> {
                assertEquals(4, packet.getRuntimeEntityId());
                var metadata = packet.getMetadata();
                assertEquals(0.0f, metadata.get(EntityDataTypes.SCALE));
                assertEquals("tesss", metadata.get(EntityDataTypes.NAME));
                assertEquals((byte) 1, metadata.get(EntityDataTypes.NAMETAG_ALWAYS_SHOW));
            });
            // because the armor stand turned invisible and has a nametag (nametag is hidden when invisible)
            assertNextPacketType(context, MoveEntityAbsolutePacket.class);

            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket(
                    "npc_team_1298",
                    Component.empty(),
                    Component.empty(),
                    Component.empty(),
                    false,
                    false,
                    NameTagVisibility.NEVER,
                    CollisionRule.NEVER,
                    TeamColor.WHITE,
                    new String[0]
                )
            );
            context.translate(
                setPlayerTeamTranslator,
                new ClientboundSetPlayerTeamPacket("npc_team_1298", TeamAction.ADD_PLAYER, new String[]{ hologramUuid.toString() }));

            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.getMetadata().put(EntityDataTypes.NAME, "§f§r§ftesss§r§f");
                packet.setRuntimeEntityId(4);
                return packet;
            });
        });
    }
}
