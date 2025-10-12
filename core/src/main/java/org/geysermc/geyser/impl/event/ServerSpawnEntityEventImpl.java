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

package org.geysermc.geyser.impl.event;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.GeyserEntityDefinition;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.UUID;

public class ServerSpawnEntityEventImpl extends ServerSpawnEntityEvent {
    private final int entityId;
    private final UUID uuid;
    private final EntityType type;
    private GeyserEntityDefinition entityDefinition;

    public ServerSpawnEntityEventImpl(@NonNull GeyserConnection connection, int entityId, @NonNull UUID uuid,
                                      @Nullable GeyserEntityDefinition entityDefinition, EntityType type) {
        super(connection);
        this.entityId = entityId;
        this.uuid = uuid;
        this.entityDefinition = entityDefinition;
        this.type = type;
    }

    public int entityId() {
        return this.entityId;
    }

    public @NonNull UUID uuid() {
        return this.uuid;
    }

    public @NonNull Identifier entityType() {
        return IdentifierImpl.of(type.name());
    }

    @Nullable
    public GeyserEntityDefinition entityDefinition() {
        return this.entityDefinition;
    }

    public void entityDefinition(@Nullable GeyserEntityDefinition entityDefinition) {
        this.entityDefinition = entityDefinition;
    }
}
