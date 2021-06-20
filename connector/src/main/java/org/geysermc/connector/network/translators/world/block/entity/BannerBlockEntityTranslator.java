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
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.nukkitx.nbt.NbtMapBuilder;
import org.geysermc.connector.network.translators.item.translators.BannerTranslator;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;

@BlockEntity(name = "Banner")
public class BannerBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {
    @Override
    public void translateTag(NbtMapBuilder builder, CompoundTag tag, int blockState) {
        int bannerColor = BlockStateValues.getBannerColor(blockState);
        if (bannerColor != -1) {
            builder.put("Base", 15 - bannerColor);
        }

        if (tag.contains("Patterns")) {
            ListTag patterns = tag.get("Patterns");
            if (patterns.equals(BannerTranslator.OMINOUS_BANNER_PATTERN)) {
                // This is an ominous banner; don't try to translate the raw patterns (it doesn't translate correctly)
                // and tell the Bedrock client that this is an ominous banner
                builder.putInt("Type", 1);
            } else {
                builder.put("Patterns", BannerTranslator.convertBannerPattern(patterns));
            }
        }

        if (tag.contains("CustomName")) {
            builder.put("CustomName", tag.get("CustomName").getValue());
        }
    }
}
