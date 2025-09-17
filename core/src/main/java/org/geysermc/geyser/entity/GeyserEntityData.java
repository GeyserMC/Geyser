/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.packet.EmotePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.api.entity.EntityData;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.properties.GeyserEntityPropertyManager;
import org.geysermc.geyser.entity.properties.type.PropertyType;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GeyserEntityData implements EntityData {

    private final GeyserSession session;
    private final Set<UUID> movementLockOwners = new HashSet<>();

    public GeyserEntityData(GeyserSession session) {
        this.session = session;
    }

    @Override
    public @NonNull CompletableFuture<@Nullable GeyserEntity> entityByJavaId(@NonNegative int javaId) {
        CompletableFuture<GeyserEntity> future = new CompletableFuture<>();
        session.ensureInEventLoop(() -> future.complete(session.getEntityCache().getEntityByJavaId(javaId)));
        return future;
    }

    @Override
    public void showEmote(@NonNull GeyserPlayerEntity emoter, @NonNull String emoteId) {
        Objects.requireNonNull(emoter, "emoter must not be null!");
        Entity entity = (Entity) emoter;
        if (entity.getSession() != session) {
            throw new IllegalStateException("Given entity must be from this session!");
        }

        EmotePacket packet = new EmotePacket();
        packet.setRuntimeEntityId(entity.getGeyserId());
        packet.setXuid("");
        packet.setPlatformId(""); // BDS sends empty
        packet.setEmoteId(emoteId);
        session.sendUpstreamPacket(packet);
    }

    @Override
    public void updateProperties(@NonNull GeyserEntity geyserEntity, @NonNull GeyserEntityProperty<?>... properties) {
        Objects.requireNonNull(properties);
        if (!(geyserEntity instanceof Entity entity)) {
            throw new IllegalArgumentException("GeyserEntity must be an instance of Entity!");
        }
        if (entity.getSession() != session) {
            throw new IllegalStateException("Given entity must be from this session!");
        }
        GeyserEntityPropertyManager propertyManager = entity.getPropertyManager();
        if (propertyManager == null) {
            throw new IllegalArgumentException("Given entity has no registered properties!");
        }

        GeyserEntityProperties propertyDefinitions = entity.getDefinition().registeredProperties();
        for (GeyserEntityProperty<?> property : properties) {
            Objects.requireNonNull(property.value(), "property value must not be null! " + property);
            int index = propertyDefinitions.getPropertyIndex(property.name());
            if (index < 0) {
                throw new IllegalArgumentException("No property with the name " + property.name() + " has been registered.");
            }

            PropertyType<?, ?> propertyType = propertyDefinitions.getProperties().get(index);
            propertyManager.addProperty(property.name(), propertyType.create(index, property.value()));
        }

        if (propertyManager.hasProperties()) {
            SetEntityDataPacket packet = new SetEntityDataPacket();
            packet.setRuntimeEntityId(entity.getGeyserId());
            propertyManager.applyFloatProperties(packet.getProperties().getFloatProperties());
            propertyManager.applyIntProperties(packet.getProperties().getIntProperties());
            session.sendUpstreamPacket(packet);
        }
    }

    @Override
    public @NonNull GeyserPlayerEntity playerEntity() {
        return session.getPlayerEntity();
    }

    @Override
    public boolean lockMovement(boolean lock, @NonNull UUID owner) {
        Objects.requireNonNull(owner, "owner must not be null!");
        if (lock) {
            movementLockOwners.add(owner);
        } else {
            movementLockOwners.remove(owner);
        }

        session.lockInputs(session.camera().isCameraLocked(), isMovementLocked());
        return isMovementLocked();
    }

    @Override
    public boolean isMovementLocked() {
        return !movementLockOwners.isEmpty();
    }

    @Override
    public void switchHands() {
        session.requestOffhandSwap();
    }
}
