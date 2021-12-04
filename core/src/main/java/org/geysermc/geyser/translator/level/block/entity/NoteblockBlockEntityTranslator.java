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

package org.geysermc.geyser.translator.level.block.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.BlockEventPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.block.BlockStateValues;

/**
 * Does not implement BlockEntityTranslator because it's only a block entity in Bedrock
 */
public class NoteblockBlockEntityTranslator {

    public static void translate(GeyserSession session, Position position) {
        int blockState = session.getGeyser().getWorldManager().getBlockAt(session, position);
        BlockEventPacket blockEventPacket = new BlockEventPacket();
        blockEventPacket.setBlockPosition(Vector3i.from(position.getX(), position.getY(), position.getZ()));
        blockEventPacket.setEventType(0);
        blockEventPacket.setEventData(BlockStateValues.getNoteblockPitch(blockState));
        session.sendUpstreamPacket(blockEventPacket);
    }

}
