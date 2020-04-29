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

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.StringTag;
import com.nukkitx.nbt.tag.Tag;
import org.geysermc.connector.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

@BlockEntity(name = "Sign", delay = true, regex = "sign")
public class SignBlockEntityTranslator extends BlockEntityTranslator {

    @Override
    public List<Tag<?>> translateTag(CompoundTag tag, BlockState blockState) {
        List<Tag<?>> tags = new ArrayList<>();

        String line1 = getOrDefault(tag.getValue().get("Text1"), "");
        String line2 = getOrDefault(tag.getValue().get("Text2"), "");
        String line3 = getOrDefault(tag.getValue().get("Text3"), "");
        String line4 = getOrDefault(tag.getValue().get("Text4"), "");

        tags.add(new StringTag("Text", MessageUtils.getBedrockMessage(Message.fromString(line1))
                + "\n" + MessageUtils.getBedrockMessage(Message.fromString(line2))
                + "\n" + MessageUtils.getBedrockMessage(Message.fromString(line3))
                + "\n" + MessageUtils.getBedrockMessage(Message.fromString(line4))
        ));

        return tags;
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = getConstantJavaTag(javaId, x, y, z);
        tag.put(new com.github.steveice10.opennbt.tag.builtin.StringTag("Text1", "{\"text\":\"\"}"));
        tag.put(new com.github.steveice10.opennbt.tag.builtin.StringTag("Text2", "{\"text\":\"\"}"));
        tag.put(new com.github.steveice10.opennbt.tag.builtin.StringTag("Text3", "{\"text\":\"\"}"));
        tag.put(new com.github.steveice10.opennbt.tag.builtin.StringTag("Text4", "{\"text\":\"\"}"));
        return tag;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        tagBuilder.stringTag("Text", "");
        return tagBuilder.buildRootTag();
    }
}
