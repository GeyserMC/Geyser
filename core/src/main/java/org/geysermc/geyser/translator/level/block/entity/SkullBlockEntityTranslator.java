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

import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMapBuilder;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.world.GeyserConvertSkullEvent;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.skin.SkinProvider;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    private static CompletableFuture<String> getTextures(CompoundTag tag) {
        CompoundTag owner = tag.get("SkullOwner");
        if (owner != null) {
            CompoundTag properties = owner.get("Properties");
            if (properties == null) {
                return SkinProvider.requestTexturesFromUsername(owner);
            }

            ListTag textures = properties.get("textures");
            LinkedHashMap<?,?> tag1 = (LinkedHashMap<?,?>) textures.get(0).getValue();
            StringTag texture = (StringTag) tag1.get("Value");
            return CompletableFuture.completedFuture(texture.getValue());
        }
        return CompletableFuture.completedFuture(null);
    }

    public static int translateSkull(GeyserSession session, CompoundTag tag, int posX, int posY, int posZ, int blockState) {
        try {
            String texturesProperty = getTextures(tag).get();
            Vector3i blockPosition = Vector3i.from(posX, posY, posZ);
            if (texturesProperty == null) {
                session.getGeyser().getLogger().debug("Custom skull with invalid SkullOwner tag: " + blockPosition + " " + tag);
                return -1;
            }
            int runtimeId = translateCustomSkull(session, blockPosition, texturesProperty, blockState);
            if (runtimeId == -1) {
                session.getSkullCache().putSkull(blockPosition, texturesProperty, blockState);
            } else {
                session.getSkullCache().putSkull(blockPosition, texturesProperty, blockState, runtimeId);
            }
            return runtimeId;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static int translateCustomSkull(GeyserSession session, Vector3i blockPosition, String texturesProperty, int blockState) {
        try {
            SkinManager.GameProfileData gameProfileData = SkinManager.GameProfileData.loadFromJson(texturesProperty);
            if (gameProfileData == null || gameProfileData.skinUrl() == null) {
                session.getGeyser().getLogger().debug("Player skull with invalid Skin tag: " + blockPosition + " Textures: " + texturesProperty);
                return -1;
            }

            String skinUrl = gameProfileData.skinUrl();
            String skinHash = skinUrl.substring(skinUrl.lastIndexOf('/') + 1);

            byte floorRotation = BlockStateValues.getSkullRotation(blockState);
            GeyserConvertSkullEvent.WallDirection wallDirection = GeyserConvertSkullEvent.WallDirection.INVALID;
            boolean onFloor = true;
            if (floorRotation == -1) {
                // Wall skull
                onFloor = false;
                int wallRotation = BlockStateValues.getSkullWallDirections().get(blockState);
                wallDirection = switch (wallRotation) {
                    case 0 -> GeyserConvertSkullEvent.WallDirection.SOUTH;
                    case 90 -> GeyserConvertSkullEvent.WallDirection.WEST;
                    case 180 -> GeyserConvertSkullEvent.WallDirection.NORTH;
                    case 270 -> GeyserConvertSkullEvent.WallDirection.EAST;
                    default -> GeyserConvertSkullEvent.WallDirection.INVALID;
                };
            }
            GeyserConvertSkullEvent event = new GeyserConvertSkullEvent(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), onFloor, wallDirection, floorRotation, skinHash);
            GeyserImpl.getInstance().getEventBus().fire(event);

            if (event.getNewBlockState() != null) {
                return session.getBlockMappings().getCustomBlockStateIds().getOrDefault(event.getNewBlockState(), -1);
            }

            if (event.isCancelled()) {
                return -1;
            }
        } catch (IOException e) {
            session.getGeyser().getLogger().debug("Player skull with invalid Skin tag: " + blockPosition + " Textures: " + texturesProperty + " Message: " + e.getMessage());
        }
        return -1;
    }
}
