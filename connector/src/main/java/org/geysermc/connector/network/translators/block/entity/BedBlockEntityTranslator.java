package org.geysermc.connector.network.translators.block.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;

import java.util.concurrent.TimeUnit;

public class BedBlockEntityTranslator {

    public static void checkForBedColor(GeyserSession session, BlockState blockState, Vector3i position) {
        byte bedcolor = BlockTranslator.getBedColor(blockState);
        // If Bed Color is not -1 then it is indeed a bed with a color.
        if (bedcolor > -1) {
            Position pos = new Position(position.getX(), position.getY(), position.getZ());
            com.nukkitx.nbt.tag.CompoundTag finalbedTag = getBedTag(bedcolor, pos);
            // Delay needed, otherwise newly placed beds will not get their color
            // Delay is not needed for beds already placed on login
            session.getConnector().getGeneralThreadPool().schedule(() ->
                            BlockEntityUtils.updateBlockEntity(session, finalbedTag, pos),
                    500,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public static com.nukkitx.nbt.tag.CompoundTag getBedTag(byte bedcolor, Position pos) {
        CompoundTagBuilder tagBuilder = CompoundTagBuilder.builder()
                .intTag("x", pos.getX())
                .intTag("y", pos.getY())
                .intTag("z", pos.getZ())
                .stringTag("id", "Bed");
        tagBuilder.byteTag("color", bedcolor);
        return tagBuilder.buildRootTag();
    }

}
