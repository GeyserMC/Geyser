package org.geysermc.connector.network.translators.block.entity;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.math.vector.Vector3i;
import org.geysermc.connector.network.session.GeyserSession;

/**
 * Any block entities that are Bedrock-only are managed through this class
 */
public abstract class BedrockOnlyBlockEntityTranslator {

    public abstract void checkForBlockEntity(GeyserSession session, BlockState blockState, Vector3i position);

}
