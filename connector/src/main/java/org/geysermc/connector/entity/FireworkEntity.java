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

package org.geysermc.connector.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.FireworkColor;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class FireworkEntity extends Entity {

    public FireworkEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }


    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 7) {
            ItemStack item = (ItemStack) entityMetadata.getValue();
            CompoundTag tag = item.getNbt();
            CompoundTag fireworks = tag.get("Fireworks");

            CompoundTagBuilder fireworksBuilder = CompoundTagBuilder.builder();
            fireworksBuilder.byteTag("Flight", (Byte) fireworks.get("Flight").getValue());

            List<com.nukkitx.nbt.tag.CompoundTag> explosions = new ArrayList<>();
            for (Tag effect : ((ListTag) fireworks.get("Explosions")).getValue()) {
                CompoundTag effectData = (CompoundTag) effect;

                try {
                    GeyserConnector.getInstance().getLogger().debug("Effect: " + new ObjectMapper().writeValueAsString(effect));
                } catch (JsonProcessingException e) { }

                CompoundTagBuilder effectBuilder = CompoundTagBuilder.builder();
                if (effectData.get("Type") != null) {
                    effectBuilder.byteTag("FireworkType", (Byte) effectData.get("Type").getValue());
                }

                if (effectData.get("Colors") != null) {
                    int[] oldColors = (int[]) effectData.get("Colors").getValue();
                    byte[] colors = new byte[oldColors.length];

                    int i = 0;
                    for (int color : oldColors) {
                        colors[i++] = FireworkColor.fromJavaID(color).getBedrockID();
                    }

                    effectBuilder.byteArrayTag("FireworkColor", colors);
                }

                if (effectData.get("FadeColors") != null) {
                    int[] oldColors = (int[]) effectData.get("FadeColors").getValue();
                    byte[] colors = new byte[oldColors.length];

                    int i = 0;
                    for (int color : oldColors) {
                        colors[i++] = FireworkColor.fromJavaID(color).getBedrockID();
                    }

                    effectBuilder.byteArrayTag("FireworkFade", colors);
                }

                if (effectData.get("Trail") != null) {
                    effectBuilder.byteTag("FireworkTrail", (Byte) effectData.get("Trail").getValue());
                }

                if (effectData.get("Flicker") != null) {
                    effectBuilder.byteTag("FireworkFlicker", (Byte) effectData.get("Flicker").getValue());
                }

                explosions.add(effectBuilder.buildRootTag());
            }

            fireworksBuilder.tag(new com.nukkitx.nbt.tag.ListTag<>("Explosions", com.nukkitx.nbt.tag.CompoundTag.class, explosions));

            metadata.put(EntityData.DISPLAY_ITEM, CompoundTagBuilder.builder().tag(fireworksBuilder.build("Fireworks")).buildRootTag());
        } else if (entityMetadata.getId() == 8 && !entityMetadata.getValue().equals(OptionalInt.empty()) && ((OptionalInt) entityMetadata.getValue()).getAsInt() == session.getPlayerEntity().getEntityId()) {
            //Checks if the firework has an entity ID (used when a player is gliding) and checks to make sure the player that is gliding is the one getting sent the packet or else every player near the gliding player will boost too.
            PlayerEntity entity = session.getPlayerEntity();
            float yaw = entity.getRotation().getX();
            float pitch = entity.getRotation().getY();
            //Uses math from NukkitX
            entity.setMotion(Vector3f.from(
                    -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 2,
                    -Math.sin(Math.toRadians(pitch)) * 2,
                    Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 2));
            //Need to update the EntityMotionPacket or else the player won't boost
            SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
            entityMotionPacket.setRuntimeEntityId(entity.getGeyserId());
            entityMotionPacket.setMotion(entity.getMotion());

            session.sendUpstreamPacket(entityMotionPacket);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }
}
