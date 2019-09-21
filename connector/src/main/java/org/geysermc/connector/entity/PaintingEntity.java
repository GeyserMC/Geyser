package org.geysermc.connector.entity;

import com.flowpowered.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.AddPaintingPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.PaintingType;

@Getter @Setter
@Accessors(chain = true)
public class PaintingEntity extends Entity {
    private static final double OFFSET = -0.46875;
    private PaintingType paintingName;
    private int direction;

    public PaintingEntity(long entityId, long geyserId, Vector3f position) {
        super(entityId, geyserId, EntityType.PAINTING, position, Vector3f.ZERO, Vector3f.ZERO);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        AddPaintingPacket addPaintingPacket = new AddPaintingPacket();
        addPaintingPacket.setUniqueEntityId(geyserId);
        addPaintingPacket.setRuntimeEntityId(geyserId);
        addPaintingPacket.setName(paintingName.getBedrockName());
        addPaintingPacket.setPosition(fixOffset(true));
        addPaintingPacket.setDirection(direction);
        session.getUpstream().sendPacket(addPaintingPacket);

        valid = true;

        GeyserLogger.DEFAULT.debug("Spawned painting on " + position);
    }

    public Vector3f fixOffset(boolean toBedrock) {
        if (toBedrock) {
            Vector3f position = super.position;
            position = position.add(0.5, 0.5, 0.5);
            double widthOffset = paintingName.getWidth() > 1 ? 0.5 : 0;
            double heightOffset = paintingName.getHeight() > 1 ? 0.5 : 0;

            switch (direction) {
                case 0: return position.add(widthOffset, heightOffset, OFFSET);
                case 1: return position.add(-OFFSET, heightOffset, widthOffset);
                case 2: return position.add(-widthOffset, heightOffset, -OFFSET);
                case 3: return position.add(OFFSET, heightOffset, -widthOffset);
            }
        }
        return position;
    }
}
