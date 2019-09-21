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

package org.geysermc.connector.entity;

import com.flowpowered.math.vector.Vector3f;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket;
import com.nukkitx.protocol.bedrock.packet.MobArmorEquipmentPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.UUID;

@Getter @Setter
public class PlayerEntity extends Entity {
    private UUID uuid;
    private String username;

    private ItemData hand;

    private ItemData helmet;
    private ItemData chestplate;
    private ItemData leggings;
    private ItemData boots;

    public PlayerEntity(GameProfile gameProfile, long entityId, long geyserId, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, EntityType.PLAYER, position, motion, rotation);

        uuid = gameProfile.getId();
        username = gameProfile.getName();
    }

    // TODO: Break this into an EquippableEntity class
    public void updateEquipment(GeyserSession session) {
        if (!valid) return;

        MobArmorEquipmentPacket armorEquipmentPacket = new MobArmorEquipmentPacket();
        armorEquipmentPacket.setRuntimeEntityId(geyserId);
        armorEquipmentPacket.setHelmet(helmet);
        armorEquipmentPacket.setChestplate(chestplate);
        armorEquipmentPacket.setLeggings(leggings);
        armorEquipmentPacket.setBoots(boots);

        session.getUpstream().sendPacket(armorEquipmentPacket);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.setRuntimeEntityId(geyserId);
        addPlayerPacket.setUniqueEntityId(geyserId);
        addPlayerPacket.setUuid(uuid);
        addPlayerPacket.setUsername(username);
        addPlayerPacket.setPlatformChatId("");
        addPlayerPacket.setPosition(position);
        addPlayerPacket.setMotion(motion);
        addPlayerPacket.setRotation(rotation);
        addPlayerPacket.setHand(hand);
        addPlayerPacket.getMetadata().putAll(getMetadata());
        addPlayerPacket.setPlayerFlags(0);
        addPlayerPacket.setCommandPermission(0);
        addPlayerPacket.setWorldFlags(0);
        addPlayerPacket.setPlayerPermission(0);
        addPlayerPacket.setCustomFlags(0);
        addPlayerPacket.setDeviceId("WIN10");

        valid = true;
        session.getUpstream().sendPacket(addPlayerPacket);
    }
}
