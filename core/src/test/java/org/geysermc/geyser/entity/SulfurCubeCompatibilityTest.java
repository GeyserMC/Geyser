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
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.entity;

import org.cloudburstmc.protocol.bedrock.codec.v1001.Bedrock_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898;
import org.cloudburstmc.protocol.bedrock.codec.v975.Bedrock_v975;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.monster.cubemob.SulfurCubeEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.geysermc.geyser.translator.protocol.java.entity.JavaAddEntityTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketMatch;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketType;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SulfurCubeCompatibilityTest {
    private static final int JAVA_ID = 2;

    private static IntStream protocolsWithoutSulfurCube() {
        return IntStream.of(Bedrock_v898.CODEC.getProtocolVersion(), Bedrock_v975.CODEC.getProtocolVersion());
    }

    @ParameterizedTest
    @MethodSource("protocolsWithoutSulfurCube")
    void olderClientsUseSlimeDefinition(int protocolVersion) {
        mockContextScoreboard(context -> {
            when(context.session().protocolVersion()).thenReturn(protocolVersion);

            spawnSulfurCube(context);

            assertSpawn(context, "minecraft:slime", VanillaEntities.SLIME.defaultBedrockDefinition(), true);
        });
    }

    @Test
    void currentClientUsesSulfurCubeDefinition() {
        mockContextScoreboard(context -> {
            when(context.session().protocolVersion()).thenReturn(Bedrock_v1001.CODEC.getProtocolVersion());

            spawnSulfurCube(context);

            assertSpawn(context, "minecraft:sulfur_cube", VanillaEntities.SULFUR_CUBE.defaultBedrockDefinition(), false);
        });
    }

    @Test
    void spawnEventCanOverrideOlderClientFallback() {
        mockContextScoreboard(context -> {
            when(context.session().protocolVersion()).thenReturn(Bedrock_v898.CODEC.getProtocolVersion());

            Identifier customIdentifier = Identifier.of("test", "sulfur_cube");
            CustomBedrockEntityDefinition customDefinition = CustomBedrockEntityDefinition.getOrCreate(customIdentifier);
            BedrockEntityDefinition previous = Registries.BEDROCK_ENTITY_DEFINITIONS.register(customIdentifier, customDefinition);
            try {
                context.mockOrSpy(GeyserImpl.class).eventBus().subscribe(
                        context.mockOrSpy(GeyserImpl.class), ServerSpawnEntityEvent.class,
                        event -> event.definition(customDefinition));

                spawnSulfurCube(context);

                SulfurCubeEntity entity = (SulfurCubeEntity) assertSpawn(
                        context, customIdentifier.toString(), customDefinition, true);
                assertDoesNotThrow(() -> entity.setBody(GeyserItemStack.EMPTY));
                assertNextPacketType(context, MobEquipmentPacket.class);
                assertNextPacketMatch(context, LevelSoundEventPacket.class,
                        packet -> assertEquals(customIdentifier.toString(), packet.getIdentifier()));
            } finally {
                if (previous == null) {
                    Registries.BEDROCK_ENTITY_DEFINITIONS.get().remove(customIdentifier);
                } else {
                    Registries.BEDROCK_ENTITY_DEFINITIONS.register(customIdentifier, previous);
                }
            }
        });
    }

    private static void spawnSulfurCube(GeyserMockContext context) {
        VanillaEntities.SULFUR_CUBE.defaultBedrockDefinition();
        context.translate(new JavaAddEntityTranslator(), new ClientboundAddEntityPacket(
                JAVA_ID, UUID.randomUUID(), EntityType.SULFUR_CUBE, 1, 2, 3, 4, 5, 6));
    }

    private static Entity assertSpawn(GeyserMockContext context, String identifier,
                                      BedrockEntityDefinition definition, boolean expectNoProperties) {
        boolean slimeFallback = "minecraft:slime".equals(identifier);
        assertNextPacketMatch(context, AddEntityPacket.class, packet -> {
            assertEquals(identifier, packet.getIdentifier());
            if (expectNoProperties) {
                assertTrue(packet.getProperties().getIntProperties().isEmpty());
                assertTrue(packet.getProperties().getFloatProperties().isEmpty());
            } else {
                assertEquals(1, packet.getProperties().getIntProperties().size());
            }
            // The slime fallback compensates for slime geometry: adults render
            // at the medium slime scale instead of scale 1, the smallest slime.
            assertEquals(slimeFallback ? 2.1f : 1f,
                    packet.getMetadata().get(EntityDataTypes.SCALE));
        });

        Entity entity = context.session().getEntityCache().getEntityByJavaId(JAVA_ID);
        assertSame(definition, entity.definition());
        return entity;
    }
}
