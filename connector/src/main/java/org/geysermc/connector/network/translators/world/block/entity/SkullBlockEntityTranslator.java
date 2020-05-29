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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.ByteTag;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.FloatTag;
import com.nukkitx.nbt.tag.Tag;
import com.nukkitx.protocol.bedrock.data.*;
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerSkinPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.utils.SkinProvider;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@BlockEntity(name = "Skull", regex = "skull")
public class SkullBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {

    public static final Map<Position, PlayerEntity> CACHED_SKULLS = new HashMap<>();
    public static final boolean allowCustomSkulls = GeyserConnector.getInstance().getConfig().isAllowCustomSkulls();

    @Override
    public boolean isBlock(BlockState blockState) {
        return BlockStateValues.getSkullVariant(blockState) != -1;
    }

    @Override
    public List<Tag<?>> translateTag(com.github.steveice10.opennbt.tag.builtin.CompoundTag tag, BlockState blockState) {
        List<Tag<?>> tags = new ArrayList<>();
        byte skullVariant = BlockStateValues.getSkullVariant(blockState);
        float rotation = BlockStateValues.getSkullRotation(blockState) * 22.5f;
        // Just in case...
        if (skullVariant == -1) skullVariant = 0;
        tags.add(new FloatTag("Rotation", rotation));
        tags.add(new ByteTag("SkullType", skullVariant));
        return tags;
    }

    @Override
    public com.github.steveice10.opennbt.tag.builtin.CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        return null;
    }

    @Override
    public CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        tagBuilder.floatTag("Rotation", 0);
        tagBuilder.byteTag("SkullType", (byte) 0);
        return tagBuilder.buildRootTag();
    }

    public static SerializedSkin getSkin(com.github.steveice10.opennbt.tag.builtin.CompoundTag tag) {
        if (tag.contains("Owner") && !CACHED_SKULLS.containsKey(tag)) {
            com.github.steveice10.opennbt.tag.builtin.CompoundTag Owner = tag.get("Owner");
            com.github.steveice10.opennbt.tag.builtin.CompoundTag Properties = Owner.get("Properties");
            ListTag Textures = Properties.get("textures");
            LinkedHashMap tag1 = (LinkedHashMap) Textures.get(0).getValue();
            StringTag texture = (StringTag) tag1.get("Value");
            byte[] decoded = Base64.getDecoder().decode(texture.getValue().getBytes());
            String url = new String(decoded);

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode jsonNodeRoot = objectMapper.readTree(url);
                JsonNode jsonNodeUrl = jsonNodeRoot.get("textures").get("SKIN").get("url");
                CompletableFuture<SkinProvider.Skin> skinCompletableFuture = SkinProvider.requestSkin(UUID.randomUUID(), jsonNodeUrl.asText(), false);
                SkinProvider.Skin skin = skinCompletableFuture.get();
                SerializedSkin serializedSkin = SerializedSkin.of(
                        "Steve", SkinProvider.SkinGeometry.getSkull().getGeometryName(), ImageData.of(skin.getSkinData()), Collections.emptyList(),
                        ImageData.EMPTY, SkinProvider.SkinGeometry.getSkull().getGeometryData(), "", true, false, false, "", UUID.randomUUID().toString()
                );
                return serializedSkin;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        else {
            return null;
        }
        return null;
    }

    public static void spawnPlayer(GeyserSession session, com.github.steveice10.opennbt.tag.builtin.CompoundTag tag, BlockState blockState) {
        SerializedSkin skin = getSkin(tag);
        float x = (int) tag.get("x").getValue() + .5f;
        float y = (int) tag.get("y").getValue() - .01f;
        float z = (int) tag.get("z").getValue() + .5f;
        float rotation = 0f;

        if(BlockStateValues.getSkullRotation(blockState) == -1) {
            y += 0.25f;
            switch (blockState.getId()) {
                case 6030: //North
                    rotation = 180f;
                    z += 0.24;
                    break;
                case 6031: //South
                    rotation = 0;
                    z -= 0.24f;
                    break;
                case 6032: //West
                    rotation = 90;
                    x += 0.24f;
                    break;
                case 6033: //East
                    rotation = 270;
                    x -= 0.24f;
                    break;
            }
        } else {
            rotation = (180f + (BlockStateValues.getSkullRotation(blockState) * 22.5f)) % 360;
        }

        UUID uuid = UUID.randomUUID();
        long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();

        PlayerEntity player = new PlayerEntity(new GameProfile(uuid, ""), 1, geyserId, Vector3f.from(x), Vector3f.from(y), Vector3f.from(z));

        //Creates a fake player to be the custom skull
        PlayerSkinPacket playerSkinPacket = new PlayerSkinPacket();
        playerSkinPacket.setOldSkinName("OldName");
        playerSkinPacket.setNewSkinName("NewName");
        playerSkinPacket.setSkin(skin);
        playerSkinPacket.setUuid(uuid);

        //Set bounding box to almost nothing so the skull is able to be broken and not cause entity to cast a shadow
        EntityDataMap metadata = new EntityDataMap();
        metadata.put(EntityData.SCALE, 1.08f);
        metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.001f);
        metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.001f);
        metadata.getOrCreateFlags().setFlag(EntityFlag.INVISIBLE, true); //Set invisible so you don't see Steve while the model changes to the custom skull

        player.setMetadata(metadata);
        player.spawnEntity(session);

        //Required for the fake player to show up at all
        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.setUuid(uuid);
        addPlayerPacket.setUsername("");
        addPlayerPacket.setRuntimeEntityId(geyserId);
        addPlayerPacket.setUniqueEntityId(geyserId);
        addPlayerPacket.setPosition(Vector3f.from(x, y, z));
        addPlayerPacket.setRotation(Vector3f.from(0f, 0f, 0f));
        addPlayerPacket.getMetadata().putAll(metadata);

        //Send all the packets back to player
        session.sendUpstreamPacket(playerSkinPacket);
        session.sendUpstreamPacket(addPlayerPacket);

        player.updateBedrockAttributes(session);
        player.moveAbsolute(session, Vector3f.from(x, y, z), rotation, 0, true, false);
        CACHED_SKULLS.put((new Position((int) tag.get("x").getValue(), (int) tag.get("y").getValue(), (int) tag.get("z").getValue())), player);
        session.getConnector().getGeneralThreadPool().schedule(() -> {
            metadata.getFlags().setFlag(EntityFlag.INVISIBLE, false);
            player.updateBedrockMetadata(session);
        }, 500, TimeUnit.MILLISECONDS); //Delay 5 seconds to give the model time to load in
    }

    public static boolean containsCustomSkull(Position position) {
        return CACHED_SKULLS.containsKey(position);
    }
}
