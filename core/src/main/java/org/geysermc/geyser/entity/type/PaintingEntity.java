/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.AddPaintingPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.PaintingType;

import java.util.UUID;

public class PaintingEntity extends Entity {
    private static final double OFFSET = -0.46875;
    private final PaintingType paintingName;
    private final int direction;

    public PaintingEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, Vector3f position, PaintingType paintingName, int direction) {
        super(session, entityId, geyserId, uuid, EntityDefinitions.PAINTING, position, Vector3f.ZERO, 0f, 0f, 0f);
        this.paintingName = paintingName;
        this.direction = direction;
    }

    @Override
    public void spawnEntity() {
        AddPaintingPacket addPaintingPacket = new AddPaintingPacket();
        addPaintingPacket.setUniqueEntityId(geyserId);
        addPaintingPacket.setRuntimeEntityId(geyserId);
        addPaintingPacket.setMotive(paintingName.getBedrockName());
        addPaintingPacket.setPosition(fixOffset());
        addPaintingPacket.setDirection(direction);
        session.sendUpstreamPacket(addPaintingPacket);

        valid = true;

        session.getGeyser().getLogger().debug("Spawned painting on " + position);
    }

    @Override
    public void updateHeadLookRotation(float headYaw) {
        // Do nothing, as head look messes up paintings
    }

    private Vector3f fixOffset() {
        Vector3f position = super.position;
        position = position.add(0.5, 0.5, 0.5);
        double widthOffset = paintingName.getWidth() > 1 ? 0.5 : 0;
        double heightOffset = paintingName.getHeight() > 1 && paintingName.getHeight() != 3 ? 0.5 : 0;

        return switch (direction) {
            case 0 -> position.add(widthOffset, heightOffset, OFFSET);
            case 1 -> position.add(-OFFSET, heightOffset, widthOffset);
            case 2 -> position.add(-widthOffset, heightOffset, -OFFSET);
            case 3 -> position.add(OFFSET, heightOffset, -widthOffset);
            default -> position;
        };
    }
}
