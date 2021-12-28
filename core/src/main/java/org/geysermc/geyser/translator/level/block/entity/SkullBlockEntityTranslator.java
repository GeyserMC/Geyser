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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMapBuilder;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.skin.SkinProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@BlockEntity(type = BlockEntityType.SKULL)
public class SkullBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {

    @Override
    public void translateTag(NbtMapBuilder builder, CompoundTag tag, int blockState) {
        byte skullVariant = BlockStateValues.getSkullVariant(blockState);
        float rotation = BlockStateValues.getSkullRotation(blockState) * 22.5f;
        // Just in case...
        if (skullVariant == -1) {
            skullVariant = 0;
        }
        builder.put("Rotation", rotation);
        builder.put("SkullType", skullVariant);
    }

    public static CompletableFuture<GameProfile> getProfile(CompoundTag tag) {
        CompoundTag owner = tag.get("SkullOwner");
        if (owner != null) {
            CompoundTag properties = owner.get("Properties");
            if (properties == null) {
                return SkinProvider.requestTexturesFromUsername(owner);
            }

            ListTag textures = properties.get("textures");
            LinkedHashMap<?,?> tag1 = (LinkedHashMap<?,?>) textures.get(0).getValue();
            StringTag texture = (StringTag) tag1.get("Value");

            List<GameProfile.Property> profileProperties = new ArrayList<>();

            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), "");
            profileProperties.add(new GameProfile.Property("textures", texture.getValue()));
            gameProfile.setProperties(profileProperties);
            return CompletableFuture.completedFuture(gameProfile);
        }
        return CompletableFuture.completedFuture(null);
    }

    public static void translateSkull(GeyserSession session, CompoundTag tag, int posX, int posY, int posZ, int blockState) {
        Vector3i blockPosition = Vector3i.from(posX, posY, posZ);
        SkullBlockEntityTranslator.getProfile(tag).whenComplete((profile, throwable) -> {
            if (profile == null) {
                session.getGeyser().getLogger().debug("Custom skull with invalid SkullOwner tag: " + blockPosition + " " + tag);
                return;
            }
            if (session.getEventLoop().inEventLoop()) {
                session.getSkullCache().putSkull(blockPosition, profile, blockState);
            } else {
                session.executeInEventLoop(() -> session.getSkullCache().putSkull(blockPosition, profile, blockState));
            }
        });
    }
}
