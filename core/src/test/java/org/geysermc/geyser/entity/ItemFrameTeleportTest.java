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
 */

package org.geysermc.geyser.entity;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.GeyserResources;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.registry.populator.DataComponentRegistryPopulator;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.java.entity.JavaEntityPositionSyncTranslator;
import org.geysermc.geyser.translator.protocol.java.entity.JavaTeleportEntityTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityPositionSyncPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContext.mockContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemFrameTeleportTest {
    private static final int JAVA_ID = 42;
    private static final long BEDROCK_ID = 100L;
    private static final Vector3f START = Vector3f.from(10, 64, 10);
    private static final Vector3f TARGET = Vector3f.from(-20.25f, 65, -10.5f);

    @BeforeAll
    static void bootstrapRegistries() {
        mockContext(() -> {
            DataComponentRegistryPopulator.load(GeyserResources.DEFAULT);
            BuiltInJavaRegistries.bootstrap();
            BuiltInJavaRegistries.freeze();
        });
    }

    static Stream<Arguments> movementCases() {
        return Stream.of(0, 1, 2).flatMap(route -> Stream.of(false, true).map(glow -> Arguments.of(route, glow)));
    }

    @ParameterizedTest(name = "route={0}, glow={1}")
    @MethodSource("movementCases")
    void relocationPreservesFrameStateAndLifecycle(int route, boolean glow) {
        mockContext(() -> {
            Fixture fixture = new Fixture(glow, true);
            NbtMap originalTag = fixture.addItem();
            fixture.move(route, TARGET);

            assertEquals(TARGET, fixture.frame.position());
            assertSame(fixture.frame, ItemFrameEntity.getItemFrameEntity(fixture.session, TARGET.toInt()));
            assertNull(ItemFrameEntity.getItemFrameEntity(fixture.session, START.toInt()));
            assertEquals(originalTag.toBuilder().putInt("x", TARGET.getFloorX()).putInt("y", TARGET.getFloorY())
                .putInt("z", TARGET.getFloorZ()).build(), fixture.assertFrameAt(TARGET.toInt()));
            UpdateBlockPacket restored = (UpdateBlockPacket) fixture.packets.getFirst();
            assertEquals(START.toInt(), restored.getBlockPosition());
            assertSame(fixture.stone, restored.getDefinition(), "Restore the real block, not unconditional air");
            verify(fixture.session.getGeyser().getWorldManager()).blockAt(fixture.session, START.toInt());
            assertFalse(fixture.packets.stream().anyMatch(packet -> packet instanceof MoveEntityAbsolutePacket || packet instanceof MoveEntityDeltaPacket));
            assertEquals(30, fixture.frame.getYaw());
            assertEquals(10, fixture.frame.getPitch());
            assertEquals(30, fixture.frame.getHeadYaw());
            assertTrue(fixture.frame.isOnGround());

            fixture.packets.clear();
            fixture.frame.updateBlock(true);
            fixture.assertFrameAt(TARGET.toInt());
            fixture.packets.clear();
            fixture.frame.despawnEntity();
            assertEquals(TARGET.toInt(), ((UpdateBlockPacket) fixture.packets.getFirst()).getBlockPosition());
            assertTrue(fixture.cache.isEmpty());
        });
    }

    @Test
    void sameCellMovementUpdatesTransformWithoutNetworkOrWorldLookup() {
        mockContext(() -> {
            Fixture fixture = new Fixture(false, true);
            Vector3f target = START.add(0.25f, 0.25f, 0.25f);
            fixture.move(1, target);
            assertEquals(target, fixture.frame.position());
            assertSame(fixture.frame, fixture.cache.get(START.toInt()));
            assertEquals(30, fixture.frame.getYaw());
            assertEquals(10, fixture.frame.getPitch());
            assertEquals(30, fixture.frame.getHeadYaw());
            assertTrue(fixture.frame.isOnGround());
            fixture.frame.moveRelativeRaw(0, 0, 0, 60, 20, 90, false);
            assertEquals(target, fixture.frame.position());
            assertEquals(60, fixture.frame.getYaw());
            assertEquals(20, fixture.frame.getPitch());
            assertEquals(90, fixture.frame.getHeadYaw());
            assertFalse(fixture.frame.isOnGround());
            assertTrue(fixture.packets.isEmpty());
            verify(fixture.session.getGeyser().getWorldManager(), never()).blockAt(any(GeyserSession.class), any(Vector3i.class));
        });
    }

    @Test
    void unspawnedFrameMovesSilentlyThenSpawnsAtDestination() {
        mockContext(() -> {
            Fixture fixture = new Fixture(false, false);
            fixture.move(0, TARGET);
            assertSame(fixture.frame, fixture.cache.get(TARGET.toInt()));
            assertNull(fixture.cache.get(START.toInt()));
            assertTrue(fixture.packets.isEmpty());
            verify(fixture.session.getGeyser().getWorldManager(), never()).blockAt(any(GeyserSession.class), any(Vector3i.class));
            fixture.frame.spawnEntity();
            fixture.assertFrameAt(TARGET.toInt());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void spawnedEmptyFrameBuildsItsTagAtDestination(boolean glow) {
        mockContext(() -> {
            Fixture fixture = new Fixture(glow, true);
            fixture.move(0, TARGET);
            NbtMap tag = fixture.assertFrameAt(TARGET.toInt());
            assertEquals(glow ? "GlowItemFrame" : "ItemFrame", tag.getString("id"));
            assertFalse(tag.containsKey("Item"));
        });
    }

    @Test
    void movingFramePreservesAnotherFrameCachedAtOrigin() {
        mockContext(() -> {
            Fixture fixture = new Fixture(false, true);
            when(fixture.session.getGeyser().getWorldManager().blockAt(fixture.session, START.toInt())).thenReturn(Blocks.AIR.defaultBlockState());
            EntitySpawnContext context = new EntitySpawnContext(fixture.session, VanillaEntities.GLOW_ITEM_FRAME, 43, 101L);
            context.position(START);
            ItemFrameEntity replacement = new ItemFrameEntity(context);
            replacement.spawnEntity();
            fixture.packets.clear();
            fixture.move(0, TARGET);
            assertSame(replacement, fixture.cache.get(START.toInt()));
            assertSame(fixture.frame, fixture.cache.get(TARGET.toInt()));
            assertTrue(fixture.packets.stream().anyMatch(packet -> packet instanceof BlockEntityDataPacket tag
                && tag.getBlockPosition().equals(START.toInt()) && tag.getData().getString("id").equals("GlowItemFrame")));
            fixture.assertFrameAt(TARGET.toInt());
        });
    }

    @Test
    void controlStationarySpawnAndDespawnRemainBlockBased() {
        mockContext(() -> {
            Fixture fixture = new Fixture(false, false);
            fixture.frame.spawnEntity();
            fixture.assertFrameAt(START.toInt());
            assertFalse(fixture.packets.stream().anyMatch(AddEntityPacket.class::isInstance));
            fixture.packets.clear();
            fixture.frame.despawnEntity();
            assertEquals(START.toInt(), ((UpdateBlockPacket) fixture.packets.getFirst()).getBlockPosition());
            assertTrue(fixture.cache.isEmpty());
        });
    }

    private static final class Fixture {
        final GeyserSession session = mock(GeyserSession.class, RETURNS_DEEP_STUBS);
        final Map<Vector3i, ItemFrameEntity> cache = new Object2ObjectOpenHashMap<>();
        final List<BedrockPacket> packets = new ArrayList<>();
        final BlockDefinition frameBlock = mock(BlockDefinition.class);
        final GeyserBedrockBlock air = mock(GeyserBedrockBlock.class);
        final GeyserBedrockBlock stone = mock(GeyserBedrockBlock.class);
        final ItemFrameEntity frame;

        Fixture(boolean glow, boolean spawned) {
            when(session.getItemFrameCache()).thenReturn(cache);
            when(session.getBlockMappings().getItemFrame(any(NbtMap.class))).thenReturn(frameBlock);
            when(session.getBlockMappings().getBedrockAir()).thenReturn(air);
            when(session.getBlockMappings().getBedrockBlock(Blocks.STONE.defaultBlockState())).thenReturn(stone);
            when(session.getGeyser().getWorldManager().blockAt(any(GeyserSession.class), any(Vector3i.class))).thenReturn(Blocks.STONE.defaultBlockState());
            doAnswer(invocation -> {
                packets.add(invocation.getArgument(0));
                return null;
            }).when(session).sendUpstreamPacket(any(BedrockPacket.class));
            EntitySpawnContext context = new EntitySpawnContext(session, glow ? VanillaEntities.GLOW_ITEM_FRAME : VanillaEntities.ITEM_FRAME, JAVA_ID, BEDROCK_ID);
            context.position(START);
            frame = new ItemFrameEntity(context);
            when(session.getEntityCache().getEntityByJavaId(JAVA_ID)).thenReturn(frame);
            if (spawned) {
                frame.spawnEntity();
            }
            packets.clear();
        }

        void move(int route, Vector3f target) {
            if (route == 2) {
                new JavaEntityPositionSyncTranslator().translate(session, new ClientboundEntityPositionSyncPacket(
                    JAVA_ID, false, target.toDouble(), null, 30, 10, true));
            } else {
                boolean relative = route == 1;
                new JavaTeleportEntityTranslator().translate(session, new ClientboundTeleportEntityPacket(
                    JAVA_ID, (relative ? target.sub(frame.position()) : target).toDouble(), Vector3f.ZERO.toDouble(), 30, 10,
                    relative ? List.of(PositionElement.X, PositionElement.Y, PositionElement.Z) : List.of(), true));
            }
        }

        NbtMap assertFrameAt(Vector3i target) {
            assertTrue(packets.stream().anyMatch(packet -> packet instanceof UpdateBlockPacket block
                && block.getDataLayer() == 0 && block.getBlockPosition().equals(target) && block.getDefinition() == frameBlock));
            NbtMap tag = packets.stream().filter(BlockEntityDataPacket.class::isInstance).map(BlockEntityDataPacket.class::cast)
                .filter(packet -> packet.getBlockPosition().equals(target)).findFirst().orElseThrow().getData();
            assertEquals(target, Vector3i.from(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
            return tag;
        }

        NbtMap addItem() {
            ItemStack held = new ItemStack(Items.DIAMOND.javaId(), 1);
            ItemData item = ItemData.builder().count(1).damage(0).definition(mock(org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition.class)).build();
            when(session.getItemMappings().getMapping(held).getBedrockIdentifier()).thenReturn("minecraft:diamond");
            try (var translator = mockStatic(ItemTranslator.class)) {
                translator.when(() -> ItemTranslator.translateToBedrock(session, held)).thenReturn(item);
                frame.setItemInFrame(new ObjectEntityMetadata<>(0, MetadataTypes.ITEM_STACK, held));
            }
            frame.setItemRotation(new IntEntityMetadata(0, MetadataTypes.INT, 3));
            frame.updateBlock(true);
            NbtMap tag = assertFrameAt(START.toInt());
            packets.clear();
            return tag;
        }
    }
}
