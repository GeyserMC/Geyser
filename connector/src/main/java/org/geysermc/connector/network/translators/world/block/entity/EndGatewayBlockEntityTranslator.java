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
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.LongTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.LinkedHashMap;

@BlockEntity(name = "EndGateway")
public class EndGatewayBlockEntityTranslator extends BlockEntityTranslator {
    @Override
    public void translateTag(NbtMapBuilder builder, CompoundTag tag, int blockState) {
        Tag ageTag = tag.get("Age");
        if (ageTag instanceof LongTag) {
            builder.put("Age", (int) ((long) ageTag.getValue()));
        }
        // Java sometimes does not provide this tag, but Bedrock crashes if it doesn't exist
        // Linked coordinates
        IntList tagsList = new IntArrayList();
        // Yes, the axis letters are capitalized
        tagsList.add(getExitPortalCoordinate(tag, "X"));
        tagsList.add(getExitPortalCoordinate(tag, "Y"));
        tagsList.add(getExitPortalCoordinate(tag, "Z"));
        builder.put("ExitPortal", new NbtList<>(NbtType.INT, tagsList));
    }

    private int getExitPortalCoordinate(CompoundTag tag, String axis) {
        // Return 0 if it doesn't exist, otherwise give proper value
        if (tag.get("ExitPortal") != null) {
            LinkedHashMap<?, ?> compoundTag = (LinkedHashMap<?, ?>) tag.get("ExitPortal").getValue();
            IntTag intTag = (IntTag) compoundTag.get(axis);
            return intTag.getValue();
        }
        return 0;
    }
}
