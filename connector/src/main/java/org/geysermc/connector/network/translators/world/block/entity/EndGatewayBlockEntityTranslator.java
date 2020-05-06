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
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.LongTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.IntTag;
import com.nukkitx.nbt.tag.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@BlockEntity(name = "EndGateway", regex = "end_gateway")
public class EndGatewayBlockEntityTranslator extends BlockEntityTranslator {

    @Override
    public List<Tag<?>> translateTag(CompoundTag tag, BlockState blockState) {
        List<Tag<?>> tags = new ArrayList<>();
        tags.add(new IntTag("Age", (int) (long) tag.get("Age").getValue()));
        // Java sometimes does not provide this tag, but Bedrock crashes if it doesn't exist
        // Linked coordinates
        List<IntTag> tagsList = new ArrayList<>();
        // Yes, the axis letters are capitalized
        tagsList.add(new IntTag("", getExitPortalCoordinate(tag, "X")));
        tagsList.add(new IntTag("", getExitPortalCoordinate(tag, "Y")));
        tagsList.add(new IntTag("", getExitPortalCoordinate(tag, "Z")));
        com.nukkitx.nbt.tag.ListTag<IntTag> exitPortal =
                new com.nukkitx.nbt.tag.ListTag<>("ExitPortal", IntTag.class, tagsList);
        tags.add(exitPortal);
        return tags;
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = getConstantJavaTag(javaId, x, y, z);
        tag.put(new LongTag("Age"));
        return tag;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        List<IntTag> tagsList = new ArrayList<>();
        tagsList.add(new IntTag("", 0));
        tagsList.add(new IntTag("", 0));
        tagsList.add(new IntTag("", 0));
        tagBuilder.listTag("ExitPortal", IntTag.class, tagsList);
        return tagBuilder.buildRootTag();
    }

    private int getExitPortalCoordinate(CompoundTag tag, String axis) {
        // Return 0 if it doesn't exist, otherwise give proper value
        if (tag.get("ExitPortal") != null) {
            LinkedHashMap compoundTag = (LinkedHashMap) tag.get("ExitPortal").getValue();
            com.github.steveice10.opennbt.tag.builtin.IntTag intTag = (com.github.steveice10.opennbt.tag.builtin.IntTag) compoundTag.get(axis);
            return intTag.getValue();
        } return 0;
    }
}
