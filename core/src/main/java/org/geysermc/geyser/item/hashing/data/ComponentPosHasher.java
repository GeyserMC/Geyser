/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.hashing.data;

import net.kyori.adventure.text.BlockNBTComponent;
import org.geysermc.geyser.item.hashing.MinecraftHasher;

public interface ComponentPosHasher {

    MinecraftHasher<BlockNBTComponent.LocalPos> LOCAL_POS_HASHER = MinecraftHasher.STRING.cast(BlockNBTComponent.LocalPos::asString);

    MinecraftHasher<BlockNBTComponent.WorldPos> WORLD_POS_HASHER = MinecraftHasher.STRING.cast(BlockNBTComponent.WorldPos::asString);

    MinecraftHasher<BlockNBTComponent.Pos> POS_HASHER = MinecraftHasher.dispatch(pos -> switch (pos) {
        case BlockNBTComponent.LocalPos ignored -> LOCAL_POS_HASHER.cast();
        case BlockNBTComponent.WorldPos ignored -> WORLD_POS_HASHER.cast();
        default -> throw new IllegalArgumentException("Unimplemented hasher for pos " + pos);
    });
}
