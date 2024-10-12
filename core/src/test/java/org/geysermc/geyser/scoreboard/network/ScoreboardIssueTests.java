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

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketType;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket;
import org.geysermc.geyser.entity.type.living.monster.EnderDragonPartEntity;
import org.geysermc.geyser.session.cache.EntityCache;
import org.geysermc.geyser.translator.protocol.java.entity.JavaRemoveEntitiesTranslator;
import org.geysermc.geyser.translator.protocol.java.entity.spawn.JavaAddExperienceOrbTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import org.junit.jupiter.api.Test;

/**
 * Tests that don't fit in a larger system (e.g. sidebar objective) that were reported on GitHub
 */
public class ScoreboardIssueTests {
    /**
     * Test for <a href="https://github.com/GeyserMC/Geyser/issues/5075">#5075</a>
     */
    @Test
    void entityWithoutUuid() {
        // experience orbs are the only known entities without an uuid, see Entity#teamIdentifier for more info
        mockContextScoreboard(context -> {
            var addExperienceOrbTranslator = new JavaAddExperienceOrbTranslator();
            var removeEntitiesTranslator = new JavaRemoveEntitiesTranslator();

            // Entity#teamIdentifier used to throw because it returned uuid.toString where uuid could be null.
            // this would result in both EntityCache#spawnEntity and EntityCache#removeEntity throwing an exception,
            // because the entity would be registered and deregistered to the scoreboard.
            assertDoesNotThrow(() -> {
                context.translate(addExperienceOrbTranslator, new ClientboundAddExperienceOrbPacket(2, 0, 0, 0, 1));

                String displayName = context.mockOrSpy(EntityCache.class).getEntityByJavaId(2).getDisplayName();
                assertEquals("entity.minecraft.experience_orb", displayName);

                context.translate(removeEntitiesTranslator, new ClientboundRemoveEntitiesPacket(new int[] { 2 }));
            });

            // we know that spawning and removing the entity should be fine
            assertNextPacketType(context, AddEntityPacket.class);
            assertNextPacketType(context, RemoveEntityPacket.class);
        });
    }

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
}
