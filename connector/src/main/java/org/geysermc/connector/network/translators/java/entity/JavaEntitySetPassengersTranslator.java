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

package org.geysermc.connector.network.translators.java.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntitySetPassengersPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.packet.SetEntityLinkPacket;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import java.util.Arrays;

@Translator(packet = ServerEntitySetPassengersPacket.class)
public class JavaEntitySetPassengersTranslator extends PacketTranslator<ServerEntitySetPassengersPacket> {

    @Override
    public void translate(ServerEntitySetPassengersPacket packet, GeyserSession session) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (entity == null) return;

        LongOpenHashSet passengers = entity.getPassengers().clone();
        boolean rider = true;
        for (long passengerId : packet.getPassengerIds()) {
            Entity passenger = session.getEntityCache().getEntityByJavaId(passengerId);
            if (passengerId == session.getPlayerEntity().getEntityId()) {
                passenger = session.getPlayerEntity();
                session.setRidingVehicleEntity(entity);
            }
            // Passenger hasn't loaded in and entity link needs to be set later
            if (passenger == null && passengerId != 0) {
                session.getEntityCache().addCachedPlayerEntityLink(passengerId, packet.getEntityId());
            }
            if (passenger == null) {
                continue;
            }

            EntityLinkData.Type type = rider ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            linkPacket.setEntityLink(new EntityLinkData(entity.getGeyserId(), passenger.getGeyserId(), type, false));
            session.sendUpstreamPacket(linkPacket);
            passengers.add(passengerId);

            // Head rotation on boats
            if (entity.getEntityType() == EntityType.BOAT) {
                passenger.getMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 1);
                passenger.getMetadata().put(EntityData.RIDER_MAX_ROTATION, 90f);
                passenger.getMetadata().put(EntityData.RIDER_MIN_ROTATION, !passengers.isEmpty() ? -90f : 0f);
            } else {
                passenger.getMetadata().remove(EntityData.RIDER_ROTATION_LOCKED);
                passenger.getMetadata().remove(EntityData.RIDER_MAX_ROTATION);
                passenger.getMetadata().remove(EntityData.RIDER_MIN_ROTATION);
            }

            passenger.updateBedrockMetadata(session);
            rider = false;
        }

        entity.setPassengers(passengers);

        for (long passengerId : entity.getPassengers()) {
            Entity passenger = session.getEntityCache().getEntityByJavaId(passengerId);
            if (passengerId == session.getPlayerEntity().getEntityId()) {
                passenger = session.getPlayerEntity();
            }
            if (passenger == null) {
                continue;
            }
            if (Arrays.stream(packet.getPassengerIds()).noneMatch(id -> id == passengerId)) {
                SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
                linkPacket.setEntityLink(new EntityLinkData(entity.getGeyserId(), passenger.getGeyserId(), EntityLinkData.Type.REMOVE, false));
                session.sendUpstreamPacket(linkPacket);
                passengers.remove(passenger.getEntityId());

                this.updateOffset(passenger, entity, session, false, false, (packet.getPassengerIds().length > 1));
            } else {
                this.updateOffset(passenger, entity, session, (packet.getPassengerIds()[0] == passengerId), true, (packet.getPassengerIds().length > 1));
            }

            // Force an update to the passenger metadata
            passenger.updateBedrockMetadata(session);
        }
    }

    private float getMountedHeightOffset(Entity mount) {
        final EntityType mountType = mount.getEntityType();
        float mountedHeightOffset = mountType.getHeight() * 0.75f;
        switch (mountType) {
            case CHICKEN:
            case SPIDER:
                mountedHeightOffset = mountType.getHeight() * 0.5f;
                break;
            case DONKEY:
            case MULE:
                mountedHeightOffset -= 0.25f;
                break;
            case LLAMA:
                mountedHeightOffset = mountType.getHeight() * 0.67f;
                break;
            case MINECART:
            case MINECART_HOPPER:
            case MINECART_TNT:
            case MINECART_CHEST:
            case MINECART_FURNACE:
            case MINECART_SPAWNER:
            case MINECART_COMMAND_BLOCK:
                mountedHeightOffset = 0;
                break;
            case BOAT:
                mountedHeightOffset = -0.1f;
                break;
            case HOGLIN:
            case ZOGLIN:
                boolean isBaby = mount.getMetadata().getFlags().getFlag(EntityFlag.BABY);
                mountedHeightOffset = mountType.getHeight() - (isBaby ? 0.2f : 0.15f);
                break;
            case PIGLIN:
                mountedHeightOffset = mountType.getHeight() * 0.92f;
                break;
            case RAVAGER:
                mountedHeightOffset = 2.1f;
                break;
            case SKELETON_HORSE:
                mountedHeightOffset -= 0.1875f;
                break;
            case STRIDER:
                mountedHeightOffset = mountType.getHeight() - 0.19f;
                break;
        }
        return mountedHeightOffset;
    }

    private float getHeightOffset(Entity passenger) {
        boolean isBaby;
        switch (passenger.getEntityType()) {
            case SKELETON:
            case STRAY:
            case WITHER_SKELETON:
                return -0.6f;
            case ARMOR_STAND:
                // Armor stand isn't a marker
                if (passenger.getMetadata().getFloat(EntityData.BOUNDING_BOX_HEIGHT) != 0.0f) {
                    return 0.1f;
                } else {
                    return 0.0f;
                }
            case ENDERMITE:
            case SILVERFISH:
                return 0.1f;
            case PIGLIN:
            case PIGLIN_BRUTE:
            case ZOMBIFIED_PIGLIN:
                isBaby = passenger.getMetadata().getFlags().getFlag(EntityFlag.BABY);
                return isBaby ? -0.05f : -0.45f;
            case ZOMBIE:
                isBaby = passenger.getMetadata().getFlags().getFlag(EntityFlag.BABY);
                return isBaby ? 0.0f : -0.45f;
            case EVOKER:
            case ILLUSIONER:
            case PILLAGER:
            case RAVAGER:
            case VINDICATOR:
            case WITCH:
                return -0.45f;
            case PLAYER:
                return 0.9f; // Java Bedrock difference
        }
        return 0f;
    }

    private void updateOffset(Entity passenger, Entity mount, GeyserSession session, boolean rider, boolean riding, boolean moreThanOneEntity) {
        passenger.getMetadata().getFlags().setFlag(EntityFlag.RIDING, riding);
        if (riding) {
            // Without the Y offset, Bedrock players will find themselves in the floor when mounting
            float mountedHeightOffset = getMountedHeightOffset(mount);
            float heightOffset = getHeightOffset(passenger);

            float xOffset = 0;
            float yOffset = mountedHeightOffset + heightOffset;
            float zOffset = 0;
            switch (mount.getEntityType()) {
                case BOAT:
                    // Without the X offset, more than one entity on a boat is stacked on top of each other
                    if (rider && moreThanOneEntity) {
                        xOffset = 0.2f;
                    } else if (moreThanOneEntity) {
                        xOffset = -0.6f;
                    }
                    break;
                case CHICKEN:
                    zOffset = -0.1f;
                    break;
                case LLAMA:
                    zOffset = -0.3f;
                    break;
            }
            Vector3f offset = Vector3f.from(xOffset, yOffset, zOffset);
            passenger.getMetadata().put(EntityData.RIDER_SEAT_POSITION, offset);
        }
        passenger.updateBedrockMetadata(session);
    }
}
