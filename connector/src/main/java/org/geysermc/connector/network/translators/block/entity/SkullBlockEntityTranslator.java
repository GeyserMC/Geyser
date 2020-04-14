package org.geysermc.connector.network.translators.block.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.StringTag;
import com.nukkitx.nbt.tag.Tag;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;
import org.geysermc.connector.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SkullBlockEntityTranslator {

    public static void checkForSkullVariant(GeyserSession session, BlockState blockState, Vector3i position) {
        byte skullVariant = BlockTranslator.getSkullVariant(blockState);
        byte rotation = BlockTranslator.getSkullRotation(blockState);
        if (skullVariant > -1) {
            Position pos = new Position(position.getX(), position.getY(), position.getZ());
            com.nukkitx.nbt.tag.CompoundTag finalSkullTag = getSkullTag(skullVariant, pos, rotation);
            // Delay needed, otherwise newly placed skulls will not appear
            // Delay is not needed for skulls already placed on login
            session.getConnector().getGeneralThreadPool().schedule(() ->
                            BlockEntityUtils.updateBlockEntity(session, finalSkullTag, pos),
                    500,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public static com.nukkitx.nbt.tag.CompoundTag getSkullTag(byte skullvariant, Position pos, byte rotation) {
        CompoundTagBuilder tagBuilder = CompoundTagBuilder.builder()
                .intTag("x", pos.getX())
                .intTag("y", pos.getY())
                .intTag("z", pos.getZ())
                .stringTag("id", "Skull")
                .floatTag("Rotation", rotation * 22.5f);
        tagBuilder.byteTag("SkullType", skullvariant);
        return tagBuilder.buildRootTag();
    }
}
