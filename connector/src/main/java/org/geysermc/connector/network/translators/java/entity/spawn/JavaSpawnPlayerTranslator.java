/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.java.entity.spawn;

import com.flowpowered.math.vector.Vector3f;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.google.gson.JsonObject;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import org.apache.commons.codec.Charsets;
import org.geysermc.api.Geyser;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.SkinProvider;

import java.util.Base64;

public class JavaSpawnPlayerTranslator extends PacketTranslator<ServerSpawnPlayerPacket> {

    @Override
    public void translate(ServerSpawnPlayerPacket packet, GeyserSession session) {
        Vector3f position = new Vector3f(packet.getX(), packet.getY(), packet.getZ());
        Vector3f rotation = new Vector3f(packet.getPitch(), packet.getYaw(), packet.getYaw());

        PlayerEntity entity = session.getEntityCache().getPlayerEntity(packet.getUUID());
        if (entity == null) {
            Geyser.getLogger().error("Haven't received PlayerListEntry packet before spawning player! We ignore the player " + packet.getUUID());
            return;
        }

        entity.setEntityId(packet.getEntityId());
        entity.setPosition(position);
        entity.setRotation(rotation);

        session.getEntityCache().spawnEntity(entity);

        // request skin and cape

        Geyser.getGeneralThreadPool().execute(() -> {
            GameProfile.Property skinProperty = entity.getProfile().getProperty("textures");

            JsonObject skinObject = SkinProvider.getGson().fromJson(new String(Base64.getDecoder().decode(skinProperty.getValue()), Charsets.UTF_8), JsonObject.class);
            JsonObject textures = skinObject.getAsJsonObject("textures");

            JsonObject skinTexture = textures.getAsJsonObject("SKIN");
            String skinUrl = skinTexture.get("url").getAsString();

            boolean isAlex = skinTexture.has("metadata");

            String capeUrl = null;
            if (textures.has("CAPE")) {
                JsonObject capeTexture = textures.getAsJsonObject("CAPE");
                capeUrl = capeTexture.get("url").getAsString();
            }

            SkinProvider.requestAndHandleSkinAndCape(entity.getUuid(), skinUrl, capeUrl)
                    .whenCompleteAsync((skinAndCape, throwable) -> {
                        SkinProvider.Skin skin = skinAndCape.getSkin();
                        SkinProvider.Cape cape = skinAndCape.getCape();

                        if (entity.getLastSkinUpdate() < skin.getRequestedOn()) {
                            Geyser.getLogger().debug("Received Skin for " + entity.getUuid() + ", updating player..");
                            entity.setLastSkinUpdate(skin.getRequestedOn());

                            PlayerListPacket.Entry updatedEntry = new PlayerListPacket.Entry(skin.getSkinOwner());
                            updatedEntry.setName(entity.getUsername());
                            updatedEntry.setEntityId(entity.getGeyserId());
                            updatedEntry.setSkinId(entity.getUuid().toString());
                            updatedEntry.setSkinData(skin.getSkinData());
                            updatedEntry.setCapeData(cape.getCapeData());
                            updatedEntry.setGeometryName("geometry.humanoid.custom" + (isAlex ? "Slim" : ""));
                            updatedEntry.setGeometryData("");
                            updatedEntry.setXuid("");
                            updatedEntry.setPlatformChatId("");

                            PlayerListPacket playerRemovePacket = new PlayerListPacket();
                            playerRemovePacket.setType(PlayerListPacket.Type.REMOVE);
                            playerRemovePacket.getEntries().add(updatedEntry);
                            session.getUpstream().sendPacket(playerRemovePacket);

                            PlayerListPacket playerAddPacket = new PlayerListPacket();
                            playerAddPacket.setType(PlayerListPacket.Type.ADD);
                            playerAddPacket.getEntries().add(updatedEntry);
                            session.getUpstream().sendPacket(playerAddPacket);
                        }
                    }).isCompletedExceptionally();
        });
    }
}
