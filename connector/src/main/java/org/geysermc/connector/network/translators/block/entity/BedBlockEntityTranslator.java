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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.Tag;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BlockEntity(name = "Bed", delay = true, regex = "bed")
public class BedBlockEntityTranslator extends BlockEntityTranslator implements BedrockOnlyBlockEntityTranslator {

    @Override
    public void checkForBlockEntity(GeyserSession session, BlockState blockState, Vector3i position) {
        byte bedcolor = BlockTranslator.getBedColor(blockState);
        System.out.println(bedcolor);
        // If Bed Color is not -1 then it is indeed a bed with a color.
        if (bedcolor > -1) {
            Position pos = new Position(position.getX(), position.getY(), position.getZ());
            com.nukkitx.nbt.tag.CompoundTag finalbedTag = getBedTag(bedcolor, pos);
            // Delay needed, otherwise newly placed beds will not get their color
            // Delay is not needed for beds already placed on login
            session.getConnector().getGeneralThreadPool().schedule(() ->
                            BlockEntityUtils.updateBlockEntity(session, finalbedTag, pos),
                    500,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public static com.nukkitx.nbt.tag.CompoundTag getBedTag(byte bedcolor, Position pos) {
        CompoundTagBuilder tagBuilder = CompoundTagBuilder.builder()
                .intTag("x", pos.getX())
                .intTag("y", pos.getY())
                .intTag("z", pos.getZ())
                .stringTag("id", "Bed");
        tagBuilder.byteTag("color", bedcolor);
        return tagBuilder.buildRootTag();
    }

    @Override
    public List<Tag<?>> translateTag(CompoundTag tag) {
        return new ArrayList<>();
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        return null;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        tagBuilder.byteTag("color", (byte) 0);
        return tagBuilder.buildRootTag();
    }
}
