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

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.ByteTag;
import com.nukkitx.nbt.tag.ListTag;
import com.nukkitx.nbt.tag.Tag;
import org.geysermc.connector.network.translators.block.BlockStateValues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BlockEntity(name = "Lectern", delay = false, regex = "lectern")
public class LecternBlockEntityTranslator extends BlockEntityTranslator {

    @Override
    public List<Tag<?>> translateTag(CompoundTag tag, BlockState blockState) {
        List<Tag<?>> list = new ArrayList<>();

        boolean hasBook = BlockStateValues.hasBook(blockState);
        list.add(new ByteTag("hasBook", hasBook));
        if (hasBook) {
            list.add(emptyBook());
            list.add(new com.nukkitx.nbt.tag.IntTag("totalPages", 0));
            list.add(new com.nukkitx.nbt.tag.IntTag("page", 0));
        }
        return list;
    }

//    CompoundTag bookTag = tag.get("Book");
//        list.add(new ByteTag("hasBook", bookTag != null));
//        if (bookTag != null) {
//        com.nukkitx.nbt.tag.CompoundTag compoundTag = Translators.getItemTranslator().translateToBedrock(bookTag);
//        list.add(new com.nukkitx.nbt.tag.CompoundTag("book", compoundTag.getValue()));
//        if (compoundTag.contains("tag") && compoundTag.contains("pages")) {
//            List<com.nukkitx.nbt.tag.CompoundTag> pages = compoundTag.getCompound("tag").getList("pages", com.nukkitx.nbt.tag.CompoundTag.class);
//            list.add(new com.nukkitx.nbt.tag.IntTag("totalPages", pages.size()));
//        }
//    }
//        if (tag.contains("page")) {
//        IntTag pageTag = tag.get("page");
//        list.add(new com.nukkitx.nbt.tag.IntTag("Page", pageTag.getValue()));
//    }
//        System.out.println(tag.toString());
//        System.out.println(list.toString());

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = getConstantJavaTag(javaId, x, y, z);
        return tag;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        return tagBuilder.buildRootTag();
    }

    public com.nukkitx.nbt.tag.CompoundTag emptyBook() {
        CompoundTagBuilder builder = CompoundTagBuilder.builder()
                .stringTag("Name", "minecraft:written_book")
                .shortTag("Damage", (short) 0)
                .byteTag("Count", (byte) 1)
                .tag(CompoundTagBuilder.builder()
                        .tag(new ListTag<>("pages", com.nukkitx.nbt.tag.CompoundTag.class, Collections.emptyList()))
                        .stringTag("title", "")
                        .stringTag("author", "").build("tag"));
        return builder.build("book");
    }
}
