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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.common.util.TriFunction;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.entity.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.JavaEntityType;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.EntityTypeDefinition;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;

import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true)
@AllArgsConstructor
public class EntitySpawnContext {
    private final GeyserSession session;
    private final EntityTypeDefinition<?> entityTypeDefinition;
    private int javaId;
    private final UUID uuid;
    private BedrockEntityDefinition bedrockEntityDefinition;
    private Vector3f position;
    private Vector3f motion;
    private float yaw;
    private float pitch;
    private float headYaw;
    private float height;
    private float width;
    private float offset;
    private long geyserId;

    public static final TriFunction<GeyserSession, UUID, EntityTypeDefinition<?>, EntitySpawnContext> DUMMY_CONTEXT = ((session, uuid, definition) ->
        new EntitySpawnContext(session, definition, 0, uuid, definition.defaultBedrockDefinition(), Vector3f.ZERO, Vector3f.ZERO, 0, 0, 0, definition.height(),
                definition.width(), definition.offset(), session.getEntityCache().getNextEntityId().incrementAndGet()));

    public static EntitySpawnContext fromPacket(GeyserSession session, EntityTypeDefinition<?> definition, ClientboundAddEntityPacket packet) {
        Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        Vector3f motion = packet.getMovement().toFloat();
        return new EntitySpawnContext(session, definition, packet.getEntityId(), packet.getUuid(), definition.defaultBedrockDefinition(),
            position, motion, packet.getYaw(), packet.getPitch(), packet.getHeadYaw(), definition.height(), definition.width(), definition.offset(), 0);
    }

    public static EntitySpawnContext inherited(GeyserSession session, EntityTypeDefinition<?> definition, Entity base, Vector3f position) {
        return new EntitySpawnContext(session, definition, 0, null, definition.defaultBedrockDefinition(), position,
            base.getMotion(), base.getYaw(), base.getPitch(), base.getHeadYaw(), definition.height(), definition.width(), definition.offset(),
            session.getEntityCache().getNextEntityId().incrementAndGet());
    }

    public void callEvent() {
        GeyserImpl.getInstance().getEventBus().fire(new ServerSpawnEntityEvent(session) {

            @Override
            public int entityId() {
                return javaId;
            }

            @Override
            public @NonNull UUID uuid() {
                return uuid;
            }

            @Override
            public @NonNull JavaEntityType entityType() {
                return entityTypeDefinition.type();
            }

            @Override
            public @Nullable GeyserEntityDefinition entityDefinition() {
                return bedrockEntityDefinition;
            }

            @Override
            public void entityDefinition(@Nullable GeyserEntityDefinition entityDefinition) {
                if (entityDefinition == null) {
                    bedrockEntityDefinition = null;
                } else {
                    if (entityDefinition instanceof BedrockEntityDefinition bed) {
                        bedrockEntityDefinition = bed;
                    } else {
                        throw new IllegalStateException("Unknown implementation of GeyserEntityDefinition");
                    }
                }
            }

            @Override
            public float width() {
                return width;
            }

            @Override
            public float height() {
                return height;
            }

            @Override
            public float offset() {
                return offset;
            }

            @Override
            public void width(@NonNegative float newWidth) {
                width = newWidth;
            }

            @Override
            public void height(@NonNegative float newHeight) {
                height = newHeight;
            }

            @Override
            public void offset(@NonNegative float newOffset) {
                offset = newOffset;
            }
        });
    }
}
