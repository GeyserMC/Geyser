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
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.skin.SkinProvider;

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

    public static int translateSkull(GeyserSession session, CompoundTag tag, Vector3i blockPosition, int blockState) {
        CompletableFuture<String> texturesFuture = getTextures(tag);
        if (texturesFuture.isDone()) {
            try {
                SkullCache.Skull skull = session.getSkullCache().putSkull(blockPosition, texturesFuture.get(), blockState);
                return skull.getCustomRuntimeId();
            } catch (InterruptedException | ExecutionException e) {
                session.getGeyser().getLogger().debug("Failed to acquire textures for custom skull: " + blockPosition + " " + tag);
                if (GeyserImpl.getInstance().getConfig().isDebugMode()) {
                    e.printStackTrace();
                }
            }
            return -1;
        }

        // SkullOwner contained a username, so we have to wait for it to be retrieved
        texturesFuture.whenComplete((texturesProperty, throwable) -> {
            if (texturesProperty == null) {
                session.getGeyser().getLogger().debug("Custom skull with invalid SkullOwner tag: " + blockPosition + " " + tag);
                return;
            }
            if (session.getEventLoop().inEventLoop()) {
                putSkull(session, blockPosition, texturesProperty, blockState);
            } else {
                session.executeInEventLoop(() -> putSkull(session, blockPosition, texturesProperty, blockState));
            }
        });
        // We don't have the textures yet, so we can't determine if a custom block was defined for this skull
        return -1;
    }

    private static void putSkull(GeyserSession session, Vector3i blockPosition, String texturesProperty, int blockState) {
        SkullCache.Skull skull = session.getSkullCache().putSkull(blockPosition, texturesProperty, blockState);
        if (skull.getCustomRuntimeId() != -1) {
            UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
            updateBlockPacket.setDataLayer(0);
            updateBlockPacket.setBlockPosition(blockPosition);
            updateBlockPacket.setRuntimeId(skull.getCustomRuntimeId());
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
            session.sendUpstreamPacket(updateBlockPacket);
        }
    }
}
