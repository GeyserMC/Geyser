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

import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.WallSkullBlock;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.skin.SkullSkinManager;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper to handle skulls more effectively - skulls have to be treated as entities since there are no
 * custom player skulls in Bedrock.
 */
public class SkullPlayerEntity extends PlayerEntity {

    @Getter
    private UUID skullUUID;

    @Getter
    private Vector3i skullPosition;

    public SkullPlayerEntity(GeyserSession session, long geyserId) {
        super(session, 0, geyserId, UUID.randomUUID(), Vector3f.ZERO, Vector3f.ZERO, 0, 0, 0, "", null);
    }

    @Override
    protected void initializeMetadata() {
        // Deliberately do not call super
        // Set bounding box to almost nothing so the skull is able to be broken and not cause entity to cast a shadow
        dirtyMetadata.put(EntityDataTypes.SCALE, 1.08f);
        dirtyMetadata.put(EntityDataTypes.HEIGHT, 0.001f);
        dirtyMetadata.put(EntityDataTypes.WIDTH, 0.001f);
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
        addPlayerPacket.getAdventureSettings().setCommandPermission(CommandPermission.ANY);
        addPlayerPacket.getAdventureSettings().setPlayerPermission(PlayerPermission.MEMBER);
        addPlayerPacket.setDeviceId("");
        addPlayerPacket.setPlatformChatId("");
        addPlayerPacket.setGameType(GameType.SURVIVAL);
        addPlayerPacket.setAbilityLayers(BASE_ABILITY_LAYER);
        addPlayerPacket.getMetadata().putFlags(flags);
        dirtyMetadata.apply(addPlayerPacket.getMetadata());

        setFlagsDirty(false);

        valid = true;
        session.sendUpstreamPacket(addPlayerPacket);
    }

    public void updateSkull(SkullCache.Skull skull) {
        skullPosition = skull.getPosition();

        if (!Objects.equals(skull.getTexturesProperty(), getTexturesProperty()) || !Objects.equals(skullUUID, skull.getUuid())) {
            // Make skull invisible as we change skins
            setFlag(EntityFlag.INVISIBLE, true);
            updateBedrockMetadata();

            skullUUID = skull.getUuid();
            setTexturesProperty(skull.getTexturesProperty());

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

        BlockState blockState = skull.getBlockState();
        if (blockState.block() instanceof WallSkullBlock) {
            y += 0.25f;
            Direction direction = blockState.getValue(Properties.HORIZONTAL_FACING);
            rotation = WallSkullBlock.getDegrees(direction);
            switch (direction) {
                case NORTH -> z += 0.24f;
                case SOUTH -> z -= 0.24f;
                case WEST -> x += 0.24f;
                case EAST -> x -= 0.24f;
            }
        } else {
            rotation = (180f + blockState.getValue(Properties.ROTATION_16, 0) * 22.5f) % 360;
        }

        moveAbsolute(Vector3f.from(x, y, z), rotation, 0, rotation, true, true);
    }
}
