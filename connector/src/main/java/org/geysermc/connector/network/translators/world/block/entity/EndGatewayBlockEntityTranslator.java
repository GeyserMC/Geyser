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
import com.github.steveice10.opennbt.tag.builtin.LongTag;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@BlockEntity(name = "EndGateway", regex = "end_gateway")
public class EndGatewayBlockEntityTranslator extends BlockEntityTranslator {

    @Override
    public Map<String, Object> translateTag(CompoundTag tag, int blockState) {
        Map<String, Object> tags = new HashMap<>();
        tags.put("Age", (int) ((long) tag.get("Age").getValue()));
        // Java sometimes does not provide this tag, but Bedrock crashes if it doesn't exist
        // Linked coordinates
        IntList tagsList = new IntArrayList();
        // Yes, the axis letters are capitalized
        tagsList.add(getExitPortalCoordinate(tag, "X"));
        tagsList.add(getExitPortalCoordinate(tag, "Y"));
        tagsList.add(getExitPortalCoordinate(tag, "Z"));
        tags.put("ExitPortal", new NbtList<>(NbtType.INT, tagsList));
        return tags;
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = getConstantJavaTag(javaId, x, y, z);
        tag.put(new LongTag("Age"));
        return tag;
    }

    @Override
    public NbtMap getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        return getConstantBedrockTag(bedrockId, x, y, z).toBuilder()
                .putList("ExitPortal", NbtType.INT, Arrays.asList(0, 0, 0))
                .build();
    }

    private int getExitPortalCoordinate(CompoundTag tag, String axis) {
        // Return 0 if it doesn't exist, otherwise give proper value
        if (tag.get("ExitPortal") != null) {
            LinkedHashMap<?, ?> compoundTag = (LinkedHashMap<?, ?>) tag.get("ExitPortal").getValue();
            com.github.steveice10.opennbt.tag.builtin.IntTag intTag = (com.github.steveice10.opennbt.tag.builtin.IntTag) compoundTag.get(axis);
            return intTag.getValue();
        } return 0;
    }
}
