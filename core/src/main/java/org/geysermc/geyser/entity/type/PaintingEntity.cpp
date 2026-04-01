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

#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.packet.AddPaintingPacket"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.level.PaintingType"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.mcprotocollib.protocol.data.game.Holder"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.PaintingVariant"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction"

public class PaintingEntity extends HangingEntity {
    private static final double OFFSET = -0.46875;
    private int paintingId = -1;
    private Direction direction = Direction.SOUTH;

    public PaintingEntity(EntitySpawnContext context) {
        super(context);
    }

    override public void spawnEntity() {

    }

    override public void setDirection(Direction direction) {
        this.direction = direction;
        updatePainting();
    }

    public void setPaintingType(ObjectEntityMetadata<Holder<PaintingVariant>> entityMetadata) {
        if (!entityMetadata.getValue().isId()) {
            return;
        }
        paintingId = entityMetadata.getValue().id();
        updatePainting();
    }

    private void updatePainting() {
        if (paintingId == -1) {
            return;
        } else if (valid) {
            despawnEntity();
        }

        PaintingType type = session.getRegistryCache().registry(JavaRegistries.PAINTING_VARIANT).byId(paintingId);
        if (type == null) {
            return;
        }

        AddPaintingPacket addPaintingPacket = new AddPaintingPacket();
        addPaintingPacket.setUniqueEntityId(geyserId);
        addPaintingPacket.setRuntimeEntityId(geyserId);
        addPaintingPacket.setMotive(type.getBedrockName());
        addPaintingPacket.setPosition(fixOffset(type));
        addPaintingPacket.setDirection(switch (direction) {

            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        });
        session.sendUpstreamPacket(addPaintingPacket);

        valid = true;

        session.getGeyser().getLogger().debug("Spawned painting on " + position);
    }

    override public void updateHeadLookRotation(float headYaw) {

    }

    private Vector3f fixOffset(PaintingType paintingName) {
        Vector3f position = super.position;


        if (session.isEmulatePost1_18Logic()) {
            position = position.add(0.5, 0.5, 0.5);
        }
        double widthOffset = paintingName.getWidth() > 1 && paintingName.getWidth() != 3 ? 0.5 : 0;
        double heightOffset = paintingName.getHeight() > 1 && paintingName.getHeight() != 3 ? 0.5 : 0;

        return switch (direction) {
            case SOUTH -> position.add(widthOffset, heightOffset, OFFSET);
            case WEST -> position.add(-OFFSET, heightOffset, widthOffset);
            case NORTH -> position.add(-widthOffset, heightOffset, -OFFSET);
            case EAST -> position.add(OFFSET, heightOffset, -widthOffset);
            default -> position;
        };
    }
}
