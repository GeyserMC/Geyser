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
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
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

        // TODO when is UP_LEFT, etc. used?
        this.inputPacket = this.inputPacket
            .withForward(up)
            .withBackward(down)
            .withLeft(left)
            .withRight(right)
            // https://mojang.github.io/bedrock-protocol-docs/html/enums.html
            // using the "raw" values allows us sending key presses even with locked input
            // There appear to be cases where the raw value is not sent - e.g. sneaking with a shield on mobile (1.21.80)
            .withJump(bedrockInput.contains(PlayerAuthInputData.JUMP_CURRENT_RAW) || bedrockInput.contains(PlayerAuthInputData.JUMP_DOWN))
            .withShift(bedrockInput.contains(PlayerAuthInputData.SNEAK_CURRENT_RAW) || bedrockInput.contains(PlayerAuthInputData.SNEAK_DOWN))
            .withSprint(bedrockInput.contains(PlayerAuthInputData.SPRINT_DOWN));

        // Send sneaking state before inputs, matches Java client
        boolean sneaking = bedrockInput.contains(PlayerAuthInputData.SNEAKING) ||
            // DESCEND_BLOCK is ONLY sent while mobile clients are descending scaffolding.
            // PERSIST_SNEAK is ALWAYS sent by mobile clients.
            // While we could use SNEAK_CURRENT_RAW, that would also be sent with locked inputs.
            // fixes https://github.com/GeyserMC/Geyser/issues/5384
            (bedrockInput.contains(PlayerAuthInputData.DESCEND_BLOCK) && bedrockInput.contains(PlayerAuthInputData.PERSIST_SNEAK));
        if (oldInputPacket.isShift() != sneaking) {
            if (sneaking) {
                session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(entity.javaId(), PlayerState.START_SNEAKING));
                session.startSneaking();
            } else {
                session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(entity.javaId(), PlayerState.STOP_SNEAKING));
                session.stopSneaking();
            }
        }

        if (oldInputPacket != this.inputPacket) { // Simple equality check is fine since we're checking for an instance change.
            session.sendDownstreamGamePacket(this.inputPacket);
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
}
