/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.world.block.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.*;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.utils.SkinProvider;
import org.geysermc.connector.utils.SkinProvider.SkinGeometry;
import org.geysermc.connector.utils.SkinUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CustomSkullTranslator{
    public static final Map<Position, PlayerEntity> CACHED_SKULLS = new HashMap<>();
    public static final boolean allowCustomSkulls = GeyserConnector.getInstance().getConfig().isAllowCustomSkulls();
    public void CustomSkullTranslator() {

    }

    public static SerializedSkin getSkin(CompoundTag tag) {
        if (tag.contains("Owner") && !CACHED_SKULLS.containsKey(tag)) {
            CompoundTag Owner = tag.get("Owner");
            CompoundTag Properties = Owner.get("Properties");
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
                        "Steve", SkinGeometry.getSkull().getGeometryName(), ImageData.of(skin.getSkinData()), Collections.emptyList(),
                        ImageData.EMPTY, SkinGeometry.getSkull().getGeometryData(), "", true, false, false, "", UUID.randomUUID().toString()
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
        return null;
    }

    public static void SpawnPlayer(GeyserSession session, CompoundTag tag, BlockState blockState) {
        SerializedSkin skin = getSkin(tag);
        float x = (int) tag.get("x").getValue() + .5f;
        float y = (int) tag.get("y").getValue() - .01f;
        float z = (int) tag.get("z").getValue() + .5f;

        float rotation = 0f;

        switch (BlockStateValues.getSkullRotation(blockState)) {
            case 0:
                rotation = 180F;
                break;
            case 1:
                rotation = 202.5F;
                break;
            case 2:
                rotation = 225F;
                break;
            case 3:
                rotation = 247.5F;
                break;
            case 4:
                rotation = 270F;
                break;
            case 5:
                rotation = 292.5F;
                break;
            case 6:
                rotation = 315F;
                break;
            case 7:
                rotation = 337.5F;
                break;
            case 8:
                rotation = 0F;
                break;
            case 9:
                rotation = 22.5F;
                break;
            case 10:
                rotation = 45F;
                break;
            case 11:
                rotation = 67.5F;
                break;
            case 12:
                rotation = 90F;
                break;
            case 13:
                rotation = 112.5F;
                break;
            case 14:
                rotation = 135F;
                break;
            case 15:
                rotation = 157.5F;
                break;
            case -1:
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
        }

        UUID uuid = UUID.randomUUID();
        long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();

        PlayerEntity player = new PlayerEntity(new GameProfile(uuid, ""), 1, geyserId, Vector3f.from(x), Vector3f.from(y), Vector3f.from(z));
        PlayerSkinPacket playerSkinPacket = new PlayerSkinPacket();
        playerSkinPacket.setOldSkinName("OldName");
        playerSkinPacket.setNewSkinName("NewName");
        playerSkinPacket.setSkin(skin);
        playerSkinPacket.setTrustedSkin(true);
        playerSkinPacket.setUuid(uuid);
        player.setValid(true);
        player.setPlayerList(true);
        EntityDataMap metadata = new EntityDataMap();
        metadata.put(EntityData.SCALE, 0f);
        metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.001f);
        metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.001f);
        player.setOnGround(true);
        player.setMetadata(metadata);
        player.spawnEntity(session);
        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.setUuid(uuid);
        addPlayerPacket.setUsername("");
        addPlayerPacket.setRuntimeEntityId(geyserId);
        addPlayerPacket.setUniqueEntityId(geyserId);
        addPlayerPacket.setPosition(Vector3f.from(x, y, z));
        System.out.println(rotation);
        addPlayerPacket.setRotation(Vector3f.from(0f, 0f, 0f));
        addPlayerPacket.setMotion(Vector3f.from(0));
        addPlayerPacket.setHand(ItemData.AIR);
        addPlayerPacket.getAdventureSettings().setCommandPermission(CommandPermission.NORMAL);
        addPlayerPacket.getAdventureSettings().setPlayerPermission(PlayerPermission.VISITOR);
        addPlayerPacket.setDeviceId("");
        addPlayerPacket.setPlatformChatId("");
        addPlayerPacket.getMetadata().putAll(metadata);
        PlayerListPacket playerListPacket = new PlayerListPacket();
        PlayerListPacket playerList = new PlayerListPacket();
        playerList.setAction(PlayerListPacket.Action.ADD);
        playerList.getEntries().add(SkinUtils.buildDefaultEntry(new GameProfile(uuid, ""), geyserId));
        session.sendUpstreamPacket(playerListPacket);
        session.sendUpstreamPacket(playerSkinPacket);
        session.sendUpstreamPacket(addPlayerPacket);
        player.getMetadata().put(EntityData.SCALE, 1.1f);
        //Delay scaling back up the entity so Steve doesn't flash on screen before it updates to the Skull model
        session.getConnector().getGeneralThreadPool().schedule(() -> player.updateBedrockMetadata(session), 100, TimeUnit.MILLISECONDS);
        player.updateBedrockAttributes(session);
        player.moveAbsolute(session, Vector3f.from(x, y, z), rotation, 0, true, false);
        CACHED_SKULLS.put((new Position((int) tag.get("x").getValue(), (int) tag.get("y").getValue(), (int) tag.get("z").getValue())), player);
    }

    public static boolean ContainsCustomSkull(Position position) {
        if (CACHED_SKULLS.containsKey(position))
            return true;
        return false;
    }

}