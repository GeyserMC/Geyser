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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.Tag;

import lombok.AllArgsConstructor;
import org.geysermc.connector.utils.BlockEntityUtils;

import java.util.List;

public abstract class BlockEntityTranslator {

    public abstract List<Tag<?>> translateTag(CompoundTag tag);

    public abstract CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z);

    public abstract com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z);

    public com.nukkitx.nbt.tag.CompoundTag getBlockEntityTag(String id, CompoundTag tag) {
        int x = Integer.parseInt(String.valueOf(tag.getValue().get("x").getValue()));
        int y = Integer.parseInt(String.valueOf(tag.getValue().get("y").getValue()));
        int z = Integer.parseInt(String.valueOf(tag.getValue().get("z").getValue()));

        CompoundTagBuilder tagBuilder = getConstantBedrockTag(BlockEntityUtils.getBedrockBlockEntityId(id), x, y, z).toBuilder();
        translateTag(tag).forEach(tagBuilder::tag);
        return tagBuilder.buildRootTag();
    }

    protected CompoundTag getConstantJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = new CompoundTag("");
        tag.put(new IntTag("x", x));
        tag.put(new IntTag("y", y));
        tag.put(new IntTag("z", z));
        tag.put(new StringTag("id", javaId));
        return tag;
    }

    protected com.nukkitx.nbt.tag.CompoundTag getConstantBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = CompoundTagBuilder.builder()
                .intTag("x", x)
                .intTag("y", y)
                .intTag("z", z)
                .stringTag("id", bedrockId);
        return tagBuilder.buildRootTag();
    }

    @SuppressWarnings("unchecked")
    protected <T> T getOrDefault(com.github.steveice10.opennbt.tag.builtin.Tag tag, T defaultValue) {
        return (tag != null && tag.getValue() != null) ? (T) tag.getValue() : defaultValue;
    }
}
