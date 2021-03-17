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

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.NbtMapBuilder;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;

import javax.annotation.Nullable;

@BlockEntity(name = "ShulkerBox")
public class ShulkerBoxBlockEntityTranslator extends BlockEntityTranslator {
    /**
     * Also used in {@link org.geysermc.connector.network.translators.inventory.translators.ShulkerInventoryTranslator}
     * where {@code tag} is passed as null.
     */
    @Override
    public void translateTag(NbtMapBuilder builder, @Nullable CompoundTag tag, int blockState) {
        byte direction = BlockStateValues.getShulkerBoxDirection(blockState);
        // Just in case...
        if (direction == -1) {
            direction = 1;
        }
        builder.put("facing", direction);
    }
}
