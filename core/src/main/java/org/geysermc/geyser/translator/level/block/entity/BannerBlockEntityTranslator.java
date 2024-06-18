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

package org.geysermc.geyser.translator.level.block.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.item.type.BannerItem;
import org.geysermc.geyser.level.block.type.BannerBlock;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

import java.util.List;

@BlockEntity(type = BlockEntityType.BANNER)
public class BannerBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {
    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, @Nullable NbtMap javaNbt, BlockState blockState) {
        if (blockState.block() instanceof BannerBlock banner) {
            bedrockNbt.putInt("Base", 15 - banner.dyeColor());
        }

        if (javaNbt == null) {
            return;
        }

        List<NbtMap> patterns = javaNbt.getList("patterns", NbtType.COMPOUND);
        if (!patterns.isEmpty()) {
            if (BannerItem.isOminous(patterns)) {
                // This is an ominous banner; don't try to translate the raw patterns (it doesn't translate correctly)
                // and tell the Bedrock client that this is an ominous banner
                bedrockNbt.putInt("Type", 1);
            } else {
                bedrockNbt.putList("Patterns", NbtType.COMPOUND, BannerItem.convertBannerPattern(patterns));
            }
        }

        String customName = javaNbt.getString("CustomName", null);
        if (customName != null) {
            bedrockNbt.putString("CustomName", customName);
        }
    }
}
