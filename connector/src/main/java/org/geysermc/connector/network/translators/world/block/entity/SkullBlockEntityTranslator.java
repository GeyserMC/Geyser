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
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityDataMap;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.utils.SkinProvider;
import org.geysermc.connector.utils.SkinUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BlockEntity(name = "Skull", regex = "skull")
public class SkullBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {
    public static final boolean ALLOW_CUSTOM_SKULLS = GeyserConnector.getInstance().getConfig().isAllowCustomSkulls();

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

    public static GameProfile getProfile(com.github.steveice10.opennbt.tag.builtin.CompoundTag tag, GeyserSession session) {
        if (tag.contains("SkullOwner")) {
            com.github.steveice10.opennbt.tag.builtin.CompoundTag owner = tag.get("SkullOwner");
            com.github.steveice10.opennbt.tag.builtin.CompoundTag Properties = owner.get("Properties");

            ListTag textures = Properties.get("textures");
            LinkedHashMap<?,?> tag1 = (LinkedHashMap<?,?>) textures.get(0).getValue();
            StringTag texture = (StringTag) tag1.get("Value");

            List<GameProfile.Property> properties = new ArrayList<>();

            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), "");
            properties.add(new GameProfile.Property("textures", texture.getValue()));
            gameProfile.setProperties(properties);
            return gameProfile;
        }
        return null;
    }

    public static void spawnPlayer(GeyserSession session, com.github.steveice10.opennbt.tag.builtin.CompoundTag tag, int blockState) {
        float x = (int) tag.get("x").getValue() + .5f;
        float y = (int) tag.get("y").getValue() - .01f;
        float z = (int) tag.get("z").getValue() + .5f;
        float rotation = 0f;

        if (BlockStateValues.getSkullRotation(blockState) == -1) {
            y += 0.25f;
            switch (BlockStateValues.getWallSkullDirection().get(blockState)) {
                case "north":
                    rotation = 180f;
                    z += 0.24;
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
            rotation = (180f + ((BlockStateValues.getSkullRotation(blockState)) * 22.5f)) % 360;
        }

        long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();

        GameProfile gameProfile = getProfile(tag, session);

        if (gameProfile == null) {
            return;
        }

        Vector3f rotationVector = Vector3f.from(rotation, 0, rotation);

        PlayerEntity player = new PlayerEntity(gameProfile, 1, geyserId, Vector3f.from(x, y, z), Vector3f.ZERO, rotationVector );
        player.setPlayerList(false);
        player.setGeometry(SkinProvider.SkinGeometry.getSkull());

        //Set bounding box to almost nothing so the skull is able to be broken and not cause entity to cast a shadow
        EntityDataMap metadata = new EntityDataMap();
        metadata.put(EntityData.SCALE, 1.08f);
        metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.001f);
        metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.001f);
        metadata.getOrCreateFlags().setFlag(EntityFlag.CAN_SHOW_NAME, false);
        metadata.getOrCreateFlags().setFlag(EntityFlag.INVISIBLE, true);

        player.setMetadata(metadata);

        // Cache entity
        session.getSkullCache().put(new Position((int) tag.get("x").getValue(), (int) tag.get("y").getValue(), (int) tag.get("z").getValue()), player);

        // Only send to session if we are initialized, otherwise it will happen then.
        if (session.getUpstream().isInitialized()) {
            player.spawnEntity(session);

            SkinUtils.requestAndHandleSkinAndCape(player, session, (skinAndCape -> session.getConnector().getGeneralThreadPool().schedule(() -> {
                player.getMetadata().getFlags().setFlag(EntityFlag.INVISIBLE, false);
                player.updateBedrockMetadata(session);
            }, 2, TimeUnit.SECONDS)));
        }
    }

    public static boolean containsCustomSkull(Position position, GeyserSession session) {
        return session.getSkullCache().containsKey(position);
    }
}
