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

package org.geysermc.geyser.entity.type.player;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.data.command.CommandPermission;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.skin.SkullSkinManager;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper to handle skulls more effectively - skulls have to be treated as entities since there are no
 * custom player skulls in Bedrock.
 */
public class SkullPlayerEntity extends PlayerEntity {

    public SkullPlayerEntity(GeyserSession session, long geyserId) {
        super(session, 0, geyserId, new GameProfile(UUID.randomUUID(), ""), Vector3f.ZERO, Vector3f.ZERO, 0, 0, 0);
        setPlayerList(false);
    }

    @Override
    protected void initializeMetadata() {
        // Deliberately do not call super
        // Set bounding box to almost nothing so the skull is able to be broken and not cause entity to cast a shadow
        dirtyMetadata.put(EntityData.SCALE, 1.08f);
        dirtyMetadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.001f);
        dirtyMetadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.001f);
        setFlag(EntityFlag.CAN_SHOW_NAME, false);
        setFlag(EntityFlag.INVISIBLE, true); // Until the skin is loaded
    }

    /**
     * Overwritten so each entity doesn't check for a linked entity
     */
    @Override
    public void spawnEntity() {
        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.setUuid(getUuid());
        addPlayerPacket.setUsername(getUsername());
        addPlayerPacket.setRuntimeEntityId(geyserId);
        addPlayerPacket.setUniqueEntityId(geyserId);
        addPlayerPacket.setPosition(position.sub(0, definition.offset(), 0));
        addPlayerPacket.setRotation(getBedrockRotation());
        addPlayerPacket.setMotion(motion);
        addPlayerPacket.setHand(hand);
        addPlayerPacket.getAdventureSettings().setCommandPermission(CommandPermission.NORMAL);
        addPlayerPacket.getAdventureSettings().setPlayerPermission(PlayerPermission.MEMBER);
        addPlayerPacket.setDeviceId("");
        addPlayerPacket.setPlatformChatId("");
        addPlayerPacket.getMetadata().putFlags(flags);
        dirtyMetadata.apply(addPlayerPacket.getMetadata());

        setFlagsDirty(false);

        valid = true;
        session.sendUpstreamPacket(addPlayerPacket);
    }

    public void free() {
        setFlag(EntityFlag.INVISIBLE, true);
        updateBedrockMetadata();

        // Move skull entity out of the way
        moveAbsolute(session.getPlayerEntity().getPosition().up(128), 0, 0, 0, false, true);
    }

    public void updateSkull(SkullCache.Skull skull) {
        if (!getTexture(getProfile()).equals(getTexture(skull.getProfile()))) {
            // Make skull invisible as we change skins
            setFlag(EntityFlag.INVISIBLE, true);
            updateBedrockMetadata();

            setProfile(skull.getProfile());
            setUsername(skull.getProfile().getName());

            SkullSkinManager.requestAndHandleSkin(this, session, (skin -> session.scheduleInEventLoop(() -> {
                // Delay to minimize split-second "player" pop-in
                setFlag(EntityFlag.INVISIBLE, false);
                updateBedrockMetadata();
            }, 250, TimeUnit.MILLISECONDS)));
        } else {
            // Just a rotation/position change
            setFlag(EntityFlag.INVISIBLE, false);
            updateBedrockMetadata();
        }

        float x = skull.getPosition().getX() + .5f;
        float y = skull.getPosition().getY() - .01f;
        float z = skull.getPosition().getZ() + .5f;
        float rotation;

        int blockState = skull.getBlockState();
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

        moveAbsolute(Vector3f.from(x, y, z), rotation, 0, rotation, true, true);
    }

    private String getTexture(GameProfile gameProfile) {
        GameProfile.Property texturesProperty = gameProfile.getProperty("textures");
        if (texturesProperty != null) {
            return texturesProperty.getValue();
        }
        return "";
    }
}
