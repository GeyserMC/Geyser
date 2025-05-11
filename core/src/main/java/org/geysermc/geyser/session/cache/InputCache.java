/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache;

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.protocol.bedrock.data.InputMode;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;

import java.util.Set;

public final class InputCache {
    private final GeyserSession session;
    private ServerboundPlayerInputPacket inputPacket = new ServerboundPlayerInputPacket(false, false, false, false, false, false, false);
    @Setter
    private boolean lastHorizontalCollision;
    private int ticksSinceLastMovePacket;
    @Getter @Setter
    private int jumpingTicks;
    @Getter @Setter
    private float jumpScale;
    @Getter @Setter
    private @MonotonicNonNull InputMode inputMode;

    public InputCache(GeyserSession session) {
        this.session = session;
    }

    public void processInputs(SessionPlayerEntity entity, PlayerAuthInputPacket packet) {
        // Input is sent to the server before packet positions, as of 1.21.2
        Set<PlayerAuthInputData> bedrockInput = packet.getInputData();
        var oldInputPacket = this.inputPacket;
        this.inputMode = packet.getInputMode();

        boolean up, down, left, right;
        if (this.inputMode == InputMode.MOUSE) {
            up = bedrockInput.contains(PlayerAuthInputData.UP);
            down = bedrockInput.contains(PlayerAuthInputData.DOWN);
            left = bedrockInput.contains(PlayerAuthInputData.LEFT);
            right = bedrockInput.contains(PlayerAuthInputData.RIGHT);
        } else {
            // The above flags don't fire TODO test console
            Vector2f analogMovement = packet.getAnalogMoveVector();
            up = analogMovement.getY() > 0;
            down = analogMovement.getY() < 0;
            left = analogMovement.getX() > 0;
            right = analogMovement.getX() < 0;
        }

        boolean sneaking = bedrockInput.contains(PlayerAuthInputData.SNEAKING);
        boolean sprint = isSprinting(bedrockInput, session.isSprinting());

        // Send sneaking state before inputs, matches Java client
        if (oldInputPacket.isShift() != sneaking) {
            if (sneaking) {
                session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(entity.javaId(), PlayerState.START_SNEAKING));
                session.startSneaking();
            } else {
                session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(entity.javaId(), PlayerState.STOP_SNEAKING));
                session.stopSneaking();
            }
        }

        // TODO when is UP_LEFT, etc. used?
        this.inputPacket = this.inputPacket
            .withForward(up)
            .withBackward(down)
            .withLeft(left)
            .withRight(right)
            // https://mojang.github.io/bedrock-protocol-docs/html/enums.html
            .withJump(bedrockInput.contains(PlayerAuthInputData.JUMP_DOWN))
            .withShift(bedrockInput.contains(PlayerAuthInputData.SNEAK_DOWN) || bedrockInput.contains(PlayerAuthInputData.SNEAK_TOGGLE_DOWN))
            .withSprint(bedrockInput.contains(PlayerAuthInputData.SPRINT_DOWN));
        if (oldInputPacket != this.inputPacket) { // Simple equality check is fine since we're checking for an instance change.
            session.sendDownstreamGamePacket(this.inputPacket);
        }

        // We're checking the session here as we need to check the current sprint state, not the keypress
        if (session.isSprinting() != sprint) {
            if (sprint) {
                // Check if the player is standing on but not surrounded by water; don't allow sprinting in that case
                // resolves <https://github.com/GeyserMC/Geyser/issues/1705>
                if (!GameProtocol.is1_21_80orHigher(session) && session.getCollisionManager().isPlayerTouchingWater() && !session.getCollisionManager().isPlayerInWater()) {
                    // Update movement speed attribute to prevent sprinting on water. This is fixed in 1.21.80+ natively.
                    UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                    attributesPacket.setRuntimeEntityId(entity.getGeyserId());
                    attributesPacket.getAttributes().addAll(entity.getAttributes().values());
                    session.sendUpstreamPacket(attributesPacket);
                } else {
                    session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(entity.javaId(), PlayerState.START_SPRINTING));
                    session.setSprinting(true);
                }
            } else {
                session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(entity.javaId(), PlayerState.STOP_SPRINTING));
                session.setSprinting(false);
            }
        }

    }

    public boolean wasJumping() {
        return this.inputPacket.isJump();
    }

    public void markPositionPacketSent() {
        this.ticksSinceLastMovePacket = 0;
    }

    public boolean shouldSendPositionReminder() {
        // NOTE: if we implement spectating entities, DO NOT TICK THIS LOGIC THEN.
        return ++this.ticksSinceLastMovePacket >= 20;
    }

    public boolean lastHorizontalCollision() {
        return lastHorizontalCollision;
    }

    // Determines whether the client is currently sprinting.
    public boolean isSprinting(Set<PlayerAuthInputData> authInputData, boolean sprinting) {
        for (PlayerAuthInputData authInput : authInputData) {
            switch (authInput) {
                case START_SPRINTING -> sprinting = true;
                case STOP_SPRINTING -> sprinting = false;
            }
        }
        return sprinting;
    }
}
