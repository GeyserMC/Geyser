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

package org.geysermc.geyser.registry.type;

import lombok.Builder;
import lombok.Value;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.geyser.level.physics.PistonBehavior;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Builder
@Value
public class BlockMapping {
    public static BlockMapping AIR;

    String javaIdentifier;
    /**
     * The block ID shared between all different block states of this block.
     * NOT the runtime ID!
     */
    int javaBlockId;

    double hardness;
    boolean canBreakWithHand;
    /**
     * The index of this collision in collision.json
     */
    int collisionIndex;
    @Nullable String pickItem;

    @Nonnull
    PistonBehavior pistonBehavior;
    boolean isBlockEntity;

    /**
     * @return the identifier without the additional block states
     */
    public String getCleanJavaIdentifier() {
        return BlockUtils.getCleanIdentifier(javaIdentifier);
    }

    /**
     * @return the corresponding Java identifier for this item
     */
    public String getItemIdentifier() {
        if (pickItem != null && !pickItem.equals("minecraft:air")) {
            // Spawners can have air as their pick item which we are not interested in.
            return pickItem;
        }

        return getCleanJavaIdentifier();
    }

    /**
     * Get the item a Java client would receive when pressing
     * the Pick Block key on a specific Java block state.
     *
     * @return The Java identifier of the item
     */
    public String getPickItem() {
        if (pickItem != null) {
            return pickItem;
        }

        return getCleanJavaIdentifier();
    }
}
