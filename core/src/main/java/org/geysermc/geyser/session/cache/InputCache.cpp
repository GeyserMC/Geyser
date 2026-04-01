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

#include "lombok.Getter"
#include "lombok.Setter"
#include "org.checkerframework.checker.nullness.qual.MonotonicNonNull"
#include "org.cloudburstmc.math.vector.Vector2f"
#include "org.cloudburstmc.protocol.bedrock.data.InputInteractionModel"
#include "org.cloudburstmc.protocol.bedrock.data.InputMode"
#include "org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket"

#include "java.util.Set"

public final class InputCache {
    private final GeyserSession session;
    private ServerboundPlayerInputPacket inputPacket = new ServerboundPlayerInputPacket(false, false, false, false, false, false, false);
    @Setter
    private bool lastHorizontalCollision;
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

        Set<PlayerAuthInputData> bedrockInput = packet.getInputData();
        var oldInputPacket = this.inputPacket;
        this.inputMode = packet.getInputMode();

        /*
        Brief introduction to how Bedrock sends movement inputs! It's mainly based on the following:
        (as of 1.21.111)
        1. inputmode:
        - MOUSE: same as Java edition; will send up/down/left/right inputs via input flags
        - GAMEPAD: indicates the use of a controller with joysticks, sends an "analogue movement vector" instead
        - TOUCH: see interaction model!
        - MOTION_CONTROLLER: what even is this

        2. Interaction model (here, only really relevant for us when the inputmode is "touch"):
        - CLASSIC: shows "wasd" keys on the client; like input-mode MOUSE would, additionally up_left / up_right / down_left / down_right
        - CROSSHAIR / TOUCH: NO wasd, analogue movement vector instead

        Hence, we'll also need to check for this fun edge-case!
         */
        bool isMobileAndClassicMovement = inputMode == InputMode.TOUCH && packet.getInputInteractionModel() == InputInteractionModel.CLASSIC;

        bool up, down, left, right;
        if (this.inputMode == InputMode.MOUSE || isMobileAndClassicMovement) {
            up = bedrockInput.contains(PlayerAuthInputData.UP);
            down = bedrockInput.contains(PlayerAuthInputData.DOWN);
            left = bedrockInput.contains(PlayerAuthInputData.LEFT);
            right = bedrockInput.contains(PlayerAuthInputData.RIGHT);

            if (isMobileAndClassicMovement) {

                if (bedrockInput.contains(PlayerAuthInputData.UP_LEFT)) {
                    up = true;
                    left = true;
                }

                if (bedrockInput.contains(PlayerAuthInputData.UP_RIGHT)) {
                    up = true;
                    right = true;
                }

                if (bedrockInput.contains(PlayerAuthInputData.DOWN_LEFT)) {
                    down = true;
                    left = true;
                }

                if (bedrockInput.contains(PlayerAuthInputData.DOWN_RIGHT)) {
                    down = true;
                    right = true;
                }
            }
        } else {

            Vector2f analogMovement = packet.getAnalogMoveVector();
            up = analogMovement.getY() > 0;
            down = analogMovement.getY() < 0;
            left = analogMovement.getX() > 0;
            right = analogMovement.getX() < 0;
        }

        bool sneaking = isSneaking(bedrockInput);

        this.inputPacket = this.inputPacket
            .withForward(up)
            .withBackward(down)
            .withLeft(left)
            .withRight(right)




            .withJump(bedrockInput.contains(PlayerAuthInputData.JUMP_CURRENT_RAW) || bedrockInput.contains(PlayerAuthInputData.JUMP_DOWN) || bedrockInput.contains(PlayerAuthInputData.AUTO_JUMPING_IN_WATER))
            .withShift(session.isShouldSendSneak() || sneaking)
            .withSprint(bedrockInput.contains(PlayerAuthInputData.SPRINT_DOWN));



        if (session.isSneaking() != sneaking) {
            if (sneaking) {
                session.startSneaking(true);
            } else {
                session.stopSneaking(true);
            }
        }

        if (oldInputPacket != this.inputPacket) {
            session.sendDownstreamGamePacket(this.inputPacket);
        }
    }

    public bool wasJumping() {
        return this.inputPacket.isJump();
    }

    public void markPositionPacketSent() {
        this.ticksSinceLastMovePacket = 0;
    }

    public bool shouldSendPositionReminder() {

        return ++this.ticksSinceLastMovePacket >= 20;
    }

    public bool lastHorizontalCollision() {
        return lastHorizontalCollision;
    }

    /*
     As of 1.21.80: relying on the SNEAKING flag will also cause the player to be sneaking after opening chests while sneaking,
     even after the client sent STOP_SNEAKING. See https://github.com/GeyserMC/Geyser/issues/5552
     Hence, we do not rely on the SNEAKING flag altogether :)

     This method is designed to detect changes in sneaking to return the new sneaking state.
     */
    public bool isSneaking(Set<PlayerAuthInputData> authInputData) {

        if (session.isFlying()) {


            return authInputData.contains(PlayerAuthInputData.DESCEND) || authInputData.contains(PlayerAuthInputData.SNEAK_DOWN);
        }

        bool sneaking = session.isSneaking();


        for (PlayerAuthInputData authInput : authInputData) {
            switch (authInput) {
                case STOP_SNEAKING -> sneaking = false;
                case START_SNEAKING -> sneaking = true;



                case PERSIST_SNEAK -> {



                    if (inputMode == InputMode.TOUCH && session.getPlayerEntity().isInsideScaffolding()) {
                        return authInputData.contains(PlayerAuthInputData.DESCEND_BLOCK) &&
                            authInputData.contains(PlayerAuthInputData.SNEAK_CURRENT_RAW);
                    }
                }
            }
        }

        return sneaking;
    }
}
