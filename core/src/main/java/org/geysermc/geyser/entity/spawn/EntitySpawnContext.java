/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.spawn;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.common.util.TriFunction;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;

import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true)
public class EntitySpawnContext {
    private final GeyserSession session;
    private final EntityDefinition<?> entityTypeDefinition;
    private int javaId;
    private final UUID uuid;
    private Vector3f position;
    private Vector3f motion;
    private float yaw;
    private float pitch;
    private float headYaw;
    private @Nullable Long geyserId;

    public static final TriFunction<GeyserSession, UUID, EntityDefinition<?>, EntitySpawnContext> DUMMY_CONTEXT = ((session, uuid, definition) -> {
        return new EntitySpawnContext(session, definition, -1, uuid);
    });

    public EntitySpawnContext(GeyserSession session, EntityDefinition<?> definition, int javaId, UUID uuid) {
        this(session, definition, javaId, uuid, Vector3f.ZERO, Vector3f.ZERO, 0, 0, 0, null);
    }

    public EntitySpawnContext(GeyserSession session, EntityDefinition<?> definition, int entityId, long geyserId) {
        this(session, definition, entityId, null, Vector3f.ZERO, Vector3f.ZERO, 0, 0, 0, geyserId);
    }

    public static EntitySpawnContext fromPacket(GeyserSession session, EntityDefinition<?> definition, ClientboundAddEntityPacket packet) {
        Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        Vector3f motion = packet.getMovement().toFloat();
        return new EntitySpawnContext(session, definition, packet.getEntityId(), packet.getUuid(), position,
            motion, packet.getYaw(), packet.getPitch(), packet.getHeadYaw(), null);
    }

    public static EntitySpawnContext inherited(GeyserSession session, EntityDefinition<?> definition, Entity parent, Vector3f position) {
        return new EntitySpawnContext(session, definition, 0, null, position, parent.getMotion(), parent.getYaw(),
            parent.getPitch(), parent.getHeadYaw(), null);
    }

    public EntitySpawnContext(GeyserSession session, EntityDefinition<?> definition, int javaId, UUID uuid, Vector3f position,
                              Vector3f motion, float yaw, float pitch, float headYaw, @Nullable Long geyserId) {
        this.session = session;
        this.entityTypeDefinition = definition;
        this.javaId = javaId;
        this.uuid = uuid;
        this.position = position.up(definition.offset());
        this.motion = motion;
        this.yaw = yaw;
        this.pitch = pitch;
        this.headYaw = headYaw;
        this.geyserId = geyserId;
    }

    // Not assigned by default - preparation for cancellable entity spawning
    public long geyserId() {
        if (geyserId == null) {
            return geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();
        }
        return geyserId;
    }
}
