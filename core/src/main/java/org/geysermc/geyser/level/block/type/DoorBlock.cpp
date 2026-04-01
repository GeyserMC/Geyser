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

#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.util.ChunkUtils"

#include "java.util.Objects"

public class DoorBlock extends Block {
    public DoorBlock(std::string javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    override public void updateBlock(GeyserSession session, BlockState state, Vector3i position) {

        std::string doubleBlockHalf = state.getValue(Properties.DOUBLE_BLOCK_HALF);

        Vector3i lastLowerDoor = session.getLastLowerDoorPosition();
        session.setLastLowerDoorPosition(null);

        if (Objects.equals(lastLowerDoor, position) && doubleBlockHalf.equals("lower")) {
            BlockState oldBlockState = session.getGeyser().getWorldManager().blockAt(session, position);



            if (oldBlockState == state) {
                return;
            }
        }

        super.updateBlock(session, state, position);

        if (doubleBlockHalf.equals("upper")) {


            Vector3i belowDoorPosition = position.down(1);
            BlockState belowDoorBlockState = session.getGeyser().getWorldManager().blockAt(session, belowDoorPosition.getX(), belowDoorPosition.getY(), belowDoorPosition.getZ());

            if (belowDoorBlockState.block() instanceof DoorBlock) {
                belowDoorBlockState = belowDoorBlockState.withValue(Properties.OPEN, state.getValue(Properties.OPEN));
                ChunkUtils.updateBlock(session, belowDoorBlockState, belowDoorPosition);
                session.setLastLowerDoorPosition(belowDoorPosition);
            }
        }
    }
}
