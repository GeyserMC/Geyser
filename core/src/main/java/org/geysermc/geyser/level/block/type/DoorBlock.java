/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.block.type;

import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.ChunkUtils;

public class DoorBlock extends Block {
    public DoorBlock(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void updateBlock(GeyserSession session, BlockState state, Vector3i position) {
        // Needed to check whether we must force the client to update the door state.
        String doubleBlockHalf = state.getValue(Properties.DOUBLE_BLOCK_HALF);

        if (!session.getGeyser().getWorldManager().hasOwnChunkCache() && doubleBlockHalf.equals("lower")) {
            BlockState oldBlockState = session.getGeyser().getWorldManager().blockAt(session, position);
            // If these are the same, it means that we already updated the lower door block (manually in the workaround below),
            // and we do not need to update the block in the cache/on the client side using the super.updateBlock() method again.
            // Otherwise, we send the door updates twice which will cause visual glitches on the client side
            if (oldBlockState == state) {
                return;
            }
        }

        super.updateBlock(session, state, position);

        if (doubleBlockHalf.equals("upper")) {
            // Update the lower door block as Bedrock client doesn't like door to be closed from the top
            // See https://github.com/GeyserMC/Geyser/issues/4358
            Vector3i belowDoorPosition = position.sub(0, 1, 0);
            BlockState belowDoorBlockState = session.getGeyser().getWorldManager().blockAt(session, belowDoorPosition.getX(), belowDoorPosition.getY(), belowDoorPosition.getZ());
            ChunkUtils.updateBlock(session, belowDoorBlockState, belowDoorPosition);
        }
    }
}
