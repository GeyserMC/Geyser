/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.type;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.components.ToolTier;
import org.geysermc.geyser.session.GeyserSession;

public class ShieldItem extends Item {
    public ShieldItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        if (tag.remove("BlockEntityTag") instanceof CompoundTag blockEntityTag) {
            if (blockEntityTag.get("Patterns") instanceof ListTag patterns) {
                for (Tag pattern : patterns) {
                    if (((CompoundTag) pattern).get("Color") instanceof IntTag color) {
                        color.setValue(15 - color.getValue());
                    }
                }
                // Bedrock looks for patterns at the root
                tag.put(patterns);
            }
            if (blockEntityTag.get("Base") instanceof IntTag base) {
                base.setValue(15 - base.getValue());
                tag.put(base);
            }
        }
    }

    @Override
    public boolean isValidRepairItem(Item other) {
        // Java Edition 1.19.3 checks the tag, but TODO check to see if we want it or are simulating what Bedrock is doing
        return ToolTier.WOODEN.getRepairIngredients().contains(other);
    }
}
