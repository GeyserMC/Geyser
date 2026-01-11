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

package org.geysermc.geyser.scoreboard.network.util;

import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.EntityCache;
import org.geysermc.geyser.session.cache.WorldCache;
import org.geysermc.geyser.session.cache.waypoint.WaypointCache;
import org.mockito.stubbing.Answer;

import java.util.UUID;
import java.util.function.Consumer;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketType;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContext.mockContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GeyserMockContextScoreboard {
    public static void mockContextScoreboard(Consumer<GeyserMockContext> geyserContext) {
        mockContext(context -> {
            createSessionSpy(context);
            geyserContext.accept(context);

            assertNoNextPacket(context);
        });
    }

    private static void createSessionSpy(GeyserMockContext context) {
        // GeyserSession has so many dependencies, it's easier to just mock it
        var session = context.mock(GeyserSession.class);

        when(session.getGeyser()).thenReturn(context.mockOrSpy(GeyserImpl.class));

        when(session.locale()).thenReturn("en_US");

        doAnswer((Answer<Void>) invocation -> {
            context.addPacket(invocation.getArgument(0, BedrockPacket.class));
            return null;
        }).when(session).sendUpstreamPacket(any());

        // SessionPlayerEntity loads stuff in like blocks, which is not what we want
        var playerEntity = context.mock(SessionPlayerEntity.class);
        when(playerEntity.geyserId()).thenReturn(1L);
        when(playerEntity.getUsername()).thenReturn("Tim203");
        when(session.getPlayerEntity()).thenReturn(playerEntity);

        var entityCache = context.spy(new EntityCache(session));
        when(session.getEntityCache()).thenReturn(entityCache);

        var worldCache = context.spy(new WorldCache(session));
        when(session.getWorldCache()).thenReturn(worldCache);

        var waypointCache = context.spy(new WaypointCache(session));
        when(session.getWaypointCache()).thenReturn(waypointCache);

        // disable global scoreboard updater
        when(worldCache.increaseAndGetScoreboardPacketsPerSecond()).thenReturn(0);
    }

    public static PlayerEntity spawnPlayerSilently(GeyserMockContext context, String username, long geyserId) {
        var player = spawnPlayer(context, username, geyserId);
        assertNextPacketType(context, AddPlayerPacket.class);
        return player;
    }

    public static PlayerEntity spawnPlayer(GeyserMockContext context, String username, long geyserId) {
        EntitySpawnContext entitySpawnContext = EntitySpawnContext.DUMMY_CONTEXT.apply(context.session(), UUID.randomUUID(), EntityDefinitions.PLAYER);
        entitySpawnContext.geyserId(geyserId);
        entitySpawnContext.javaId((int) geyserId);
        var playerEntity = spy(new PlayerEntity(entitySpawnContext, username, null));

        var entityCache = context.mockOrSpy(EntityCache.class);
        entityCache.addPlayerEntity(playerEntity);
        // called when the player spawns
        entityCache.spawnEntity(playerEntity);

        return playerEntity;
    }
}
