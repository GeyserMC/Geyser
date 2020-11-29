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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.player.SkullPlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.skin.SkinProvider;
import org.geysermc.connector.skin.SkullSkinManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@BlockEntity(name = "Skull", regex = "skull")
public class SkullBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {
    public static boolean ALLOW_CUSTOM_SKULLS;

    @Override
    public boolean isBlock(int blockState) {
        return BlockStateValues.getSkullVariant(blockState) != -1;
    }

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

    public static GameProfile getProfile(CompoundTag tag) {
        if (tag.contains("SkullOwner")) {
            CompoundTag owner = tag.get("SkullOwner");
            CompoundTag properties = owner.get("Properties");
            if (properties == null) {
                try {
                    CompletableFuture<GameProfile> gameProfile = SkinProvider.requestTexturesFromUsername(owner);
                    return gameProfile.get();
                }  catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            ListTag textures = properties.get("textures");
            LinkedHashMap<?,?> tag1 = (LinkedHashMap<?,?>) textures.get(0).getValue();
            StringTag texture = (StringTag) tag1.get("Value");

            List<GameProfile.Property> profileProperties = new ArrayList<>();

            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), "");
            profileProperties.add(new GameProfile.Property("textures", texture.getValue()));
            gameProfile.setProperties(profileProperties);
            return gameProfile;
        }
        return null;
    }

    public static void spawnPlayer(GeyserSession session, CompoundTag tag, int blockState) {
        int posX = (int) tag.get("x").getValue();
        int posY = (int) tag.get("y").getValue();
        int posZ = (int) tag.get("z").getValue();
        float x = posX + .5f;
        float y = posY - .01f;
        float z = posZ + .5f;
        float rotation = 0f;

        byte floorRotation = BlockStateValues.getSkullRotation(blockState);
        if (floorRotation == -1) {
            // Wall skull
            y += 0.25f;
            switch (BlockStateValues.getSkullWallDirections().get(blockState)) {
                case "north":
                    rotation = 180f;
                    z += 0.24f;
                    break;
                case "south":
                    rotation = 0;
                    z -= 0.24f;
                    break;
                case "west":
                    rotation = 90;
                    x += 0.24f;
                    break;
                case "east":
                    rotation = 270;
                    x -= 0.24f;
                    break;
            }
        } else {
            rotation = (180f + (floorRotation * 22.5f)) % 360;
        }

        Vector3i blockPosition = Vector3i.from(posX, posY, posZ);
        long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();

        GameProfile gameProfile = getProfile(tag);
        if (gameProfile == null) {
            session.getConnector().getLogger().debug("Custom skull with invalid SkullOwner tag: " + blockPosition.toString() + " " + tag.toString());
            return;
        }

        Vector3f rotationVector = Vector3f.from(rotation, 0, rotation);

        SkullPlayerEntity existingSkull = session.getSkullCache().get(blockPosition);
        if (existingSkull != null) {
            // Ensure that two skulls can't spawn on the same point
            existingSkull.despawnEntity(session, blockPosition);
        }

        SkullPlayerEntity player = new SkullPlayerEntity(gameProfile, geyserId, Vector3f.from(x, y, z), rotationVector);
        player.setBlockState(blockState);

        // Cache entity
        session.getSkullCache().put(blockPosition, player);

        // Only send to session if we are initialized, otherwise it will happen then.
        if (session.getUpstream().isInitialized()) {
            player.spawnEntity(session);

            SkullSkinManager.requestAndHandleSkinAndCape(player, session, (skinAndCape -> session.getConnector().getGeneralThreadPool().schedule(() -> {
                // Delay to minimize split-second "player" pop-in
                player.getMetadata().getFlags().setFlag(EntityFlag.INVISIBLE, false);
                player.updateBedrockMetadata(session);
            }, 250, TimeUnit.MILLISECONDS)));
        }
    }
}
