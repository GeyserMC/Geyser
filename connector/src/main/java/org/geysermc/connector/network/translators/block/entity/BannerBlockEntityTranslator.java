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
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.StringTag;
import com.nukkitx.nbt.tag.Tag;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BannerBlockEntityTranslator extends BlockEntityTranslator {

    public static void checkForBannerColor(GeyserSession session, BlockState blockState, Vector3i position) {
        int bannercolor = BlockTranslator.getBannerColor(blockState);
        // If Banner Color is not -1 then it is indeed a Banner with a color.
        if (bannercolor > -1) {
            Position pos = new Position(position.getX(), position.getY(), position.getZ());
            com.nukkitx.nbt.tag.CompoundTag finalbannerTag = getBannerTag(bannercolor, pos);
            GeyserConnector.getInstance().getLogger().debug("FBanner: " + finalbannerTag);

            // Delay needed, otherwise newly placed beds will not get their color
            // Delay is not needed for beds already placed on login
            session.getConnector().getGeneralThreadPool().schedule(() ->
                            BlockEntityUtils.updateBlockEntity(session, finalbannerTag, pos),
                    500,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public static com.nukkitx.nbt.tag.CompoundTag getBannerTag(int bannercolor, Position pos) {
        CompoundTagBuilder tagBuilder = CompoundTagBuilder.builder()
                .intTag("x", pos.getX())
                .intTag("y", pos.getY())
                .intTag("z", pos.getZ())
                .stringTag("id", "Banner");
        tagBuilder.intTag("Base", 15 - bannercolor);
        return tagBuilder.buildRootTag();
    }

    @Override
    public List<Tag<?>> translateTag(CompoundTag tag) {
        GeyserConnector.getInstance().getLogger().debug("BTag: " + tag);

        List<Tag<?>> tags = new ArrayList<>();
        ListTag patterns = tag.get("Patterns");
        List<com.nukkitx.nbt.tag.CompoundTag> tagsList = new ArrayList<>();
        if (tag.contains("Patterns")) {
            for (com.github.steveice10.opennbt.tag.builtin.Tag patternTag : patterns.getValue()) {
                tagsList.add(getPattern((CompoundTag) patternTag));
            }
            com.nukkitx.nbt.tag.ListTag<com.nukkitx.nbt.tag.CompoundTag> bedrockPatterns =
                    new com.nukkitx.nbt.tag.ListTag<>("Patterns", com.nukkitx.nbt.tag.CompoundTag.class, tagsList);
            tags.add(bedrockPatterns);
        }
        if (tag.contains("CustomName")) {
            tags.add(new StringTag("CustomName", (String) tag.get("CustomName").getValue()));
        }

        // This needs to be mapped in blocks.json as something like banner_color
        // But I cant get that to work, hardcoded to red for now
        //tags.add(new IntTag("Base", 1));

        return tags;
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = getConstantJavaTag(javaId, x, y, z);
        tag.put(new ListTag("Patterns"));
        return tag;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        tagBuilder.listTag("Patterns", com.nukkitx.nbt.tag.CompoundTag.class, new ArrayList<>());
        return tagBuilder.buildRootTag();
    }

    protected com.nukkitx.nbt.tag.CompoundTag getPattern(CompoundTag pattern) {
        // Pattern colour values are inverted on bedrock for some reason
        // So we take the value we get from 15 to invert it
        return CompoundTagBuilder.builder()
                .intTag("Color", 15 - (int) pattern.get("Color").getValue())
                .stringTag("Pattern", (String) pattern.get("Pattern").getValue())
                .buildRootTag();
    }
}
