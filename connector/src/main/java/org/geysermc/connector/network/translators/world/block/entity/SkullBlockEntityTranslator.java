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

import com.nukkitx.nbt.NbtMap;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;

import java.util.HashMap;
import java.util.Map;

@BlockEntity(name = "Skull", regex = "skull")
public class SkullBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {

    @Override
    public boolean isBlock(int blockState) {
        return BlockStateValues.getSkullVariant(blockState) != -1;
    }

    @Override
    public Map<String, Object> translateTag(com.github.steveice10.opennbt.tag.builtin.CompoundTag tag, int blockState) {
        Map<String, Object> tags = new HashMap<>();
        byte skullVariant = BlockStateValues.getSkullVariant(blockState);
        float rotation = BlockStateValues.getSkullRotation(blockState) * 22.5f;
        // Just in case...
        if (skullVariant == -1) skullVariant = 0;
        tags.put("Rotation", rotation);
        tags.put("SkullType", skullVariant);
        return tags;
    }

    @Override
    public com.github.steveice10.opennbt.tag.builtin.CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        return null;
    }

    @Override
    public NbtMap getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        return getConstantBedrockTag(bedrockId, x, y, z).toBuilder()
                .putFloat("Rotation", 0f)
                .putByte("SkullType", (byte) 0)
                .build();
    }
}
