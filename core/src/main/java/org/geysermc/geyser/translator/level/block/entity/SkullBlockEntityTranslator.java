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

package org.geysermc.geyser.translator.level.block.entity;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.type.player.SkullPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.skin.SkullSkinManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    public static void spawnPlayer(GeyserSession session, CompoundTag tag, int posX, int posY, int posZ, int blockState) {
        float x = posX + .5f;
        float y = posY - .01f;
        float z = posZ + .5f;
        float rotation;

        byte floorRotation = BlockStateValues.getSkullRotation(blockState);
        if (floorRotation == -1) {
            // Wall skull
            y += 0.25f;
            rotation = BlockStateValues.getSkullWallDirections().get(blockState);
            switch ((int) rotation) {
                case 180 -> z += 0.24f; // North
                case 0 -> z -= 0.24f; // South
                case 90 -> x += 0.24f; // West
                case 270 -> x -= 0.24f; // East
            }
        } else {
            rotation = (180f + (floorRotation * 22.5f)) % 360;
        }

        Vector3i blockPosition = Vector3i.from(posX, posY, posZ);
        Vector3f entityPosition = Vector3f.from(x, y, z);

        getProfile(tag).whenComplete((gameProfile, throwable) -> {
            if (gameProfile == null) {
                session.getGeyser().getLogger().debug("Custom skull with invalid SkullOwner tag: " + blockPosition + " " + tag);
                return;
            }

            if (session.getEventLoop().inEventLoop()) {
                spawnPlayer(session, gameProfile, blockPosition, entityPosition, rotation, blockState);
            } else {
                session.executeInEventLoop(() -> spawnPlayer(session, gameProfile, blockPosition, entityPosition, rotation, blockState));
            }
        });
    }

    private static void spawnPlayer(GeyserSession session, GameProfile profile, Vector3i blockPosition,
                                    Vector3f entityPosition, float rotation, int blockState) {
        long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();

        SkullPlayerEntity existingSkull = session.getSkullCache().get(blockPosition);
        if (existingSkull != null) {
            // Ensure that two skulls can't spawn on the same point
            existingSkull.despawnEntity(blockPosition);
        }

        SkullPlayerEntity player = new SkullPlayerEntity(session, geyserId, profile, entityPosition, rotation, blockState);

        // Cache entity
        session.getSkullCache().put(blockPosition, player);

        player.spawnEntity();

        SkullSkinManager.requestAndHandleSkin(player, session, (skin -> session.scheduleInEventLoop(() -> {
            // Delay to minimize split-second "player" pop-in
            player.setFlag(EntityFlag.INVISIBLE, false);
            player.updateBedrockMetadata();
        }, 250, TimeUnit.MILLISECONDS)));
    }
}
