/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.block.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;

import java.util.concurrent.TimeUnit;

@BlockEntity(name = "", delay = true)
public class SkullBlockEntityTranslator extends BedrockOnlyBlockEntityTranslator {

    @Override
    public void checkForBlockEntity(GeyserSession session, BlockState blockState, Vector3i position) {
        byte skullVariant = BlockTranslator.getSkullVariant(blockState);
        byte rotation = BlockTranslator.getSkullRotation(blockState);
        if (skullVariant > -1) {
            Position pos = new Position(position.getX(), position.getY(), position.getZ());
            CompoundTag finalSkullTag = getSkullTag(skullVariant, pos, rotation);
            // Delay needed, otherwise newly placed skulls will not appear
            // Delay is not needed for skulls already placed on login
            session.getConnector().getGeneralThreadPool().schedule(() ->
                            BlockEntityUtils.updateBlockEntity(session, finalSkullTag, pos),
                    500,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public static CompoundTag getSkullTag(byte skullvariant, Position pos, byte rotation) {
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
