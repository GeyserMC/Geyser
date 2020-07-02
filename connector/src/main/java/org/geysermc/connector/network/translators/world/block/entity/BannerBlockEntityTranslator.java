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

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.IntTag;
import com.nukkitx.nbt.tag.StringTag;
import com.nukkitx.nbt.tag.Tag;
import org.geysermc.connector.network.translators.item.translators.BannerTranslator;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;

import java.util.ArrayList;
import java.util.List;

@BlockEntity(name = "Banner", regex = "banner")
public class BannerBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {

    @Override
    public boolean isBlock(int blockState) {
        return BlockStateValues.getBannerColor(blockState) != -1;
    }

    @Override
    public List<Tag<?>> translateTag(CompoundTag tag, int blockState) {
        List<Tag<?>> tags = new ArrayList<>();

        int bannerColor = BlockStateValues.getBannerColor(blockState);
        if (bannerColor != -1) {
            tags.add(new IntTag("Base", 15 - bannerColor));
        }

        if (tag.contains("Patterns")) {
            ListTag patterns = tag.get("Patterns");
            tags.add(BannerTranslator.convertBannerPattern(patterns));
        }

        if (tag.contains("CustomName")) {
            tags.add(new StringTag("CustomName", (String) tag.get("CustomName").getValue()));
        }

        return tags;
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = getConstantJavaTag(javaId, x, y, z);
        tag.put(new ListTag("Patterns"));
        return tag;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        tagBuilder.listTag("Patterns", com.nukkitx.nbt.tag.CompoundTag.class, new ArrayList<>());
        return tagBuilder.buildRootTag();
    }
}
