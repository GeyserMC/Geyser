/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;

public class ExpOrbEntity extends Entity {

    public ExpOrbEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> entityDefinition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        this(session, 1, entityId, geyserId, position);
    }

    public ExpOrbEntity(GeyserSession session, int amount, int entityId, long geyserId, Vector3f position) {
        super(session, entityId, geyserId, null, EntityDefinitions.EXPERIENCE_ORB, position, Vector3f.ZERO, 0, 0, 0);

        this.dirtyMetadata.put(EntityDataTypes.TRADE_EXPERIENCE, amount);
    }
}
