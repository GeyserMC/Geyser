/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.impl.camera;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.CameraShakeAction;
import org.cloudburstmc.protocol.bedrock.data.CameraShakeType;
import org.cloudburstmc.protocol.bedrock.data.camera.CameraEase;
import org.cloudburstmc.protocol.bedrock.data.camera.CameraFadeInstruction;
import org.cloudburstmc.protocol.bedrock.data.camera.CameraSetInstruction;
import org.cloudburstmc.protocol.bedrock.packet.CameraInstructionPacket;
import org.cloudburstmc.protocol.bedrock.packet.CameraShakePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerFogPacket;
import org.geysermc.geyser.api.bedrock.camera.*;
import org.geysermc.geyser.session.GeyserSession;

import java.util.*;

public class GeyserCameraData implements CameraData {

    private final GeyserSession session;

    @Getter
    private CameraPerspective cameraPerspective;

    /**
     * All fog effects that are currently applied to the client.
     */
    private final Set<String> appliedFog = new HashSet<>();

    private final Set<UUID> cameraLockOwners = new HashSet<>();

    public GeyserCameraData(GeyserSession session) {
        this.session = session;
    }

    @Override
    public void clearCameraInstructions() {
        this.cameraPerspective = null;
        CameraInstructionPacket packet = new CameraInstructionPacket();
        packet.setClear(true);
        session.sendUpstreamPacket(packet);
    }

    @Override
    public void forceCameraPerspective(@NonNull CameraPerspective perspective) {
        Objects.requireNonNull(perspective, "perspective cannot be null!");

        if (perspective == cameraPerspective) {
            return; // nothing to do
        }

        this.cameraPerspective = perspective;
        CameraInstructionPacket packet = new CameraInstructionPacket();
        CameraSetInstruction setInstruction = new CameraSetInstruction();

        if (perspective == CameraPerspective.FREE) {
            throw new IllegalArgumentException("Cannot force a stationary camera (CameraPerspective#FREE) on the player!" +
                    "Send a CameraPosition with an exact position instead");
        }

        setInstruction.setPreset(CameraDefinitions.getById(perspective.ordinal()));
        packet.setSetInstruction(setInstruction);
        session.sendUpstreamPacket(packet);
    }

    @Override
    public @Nullable CameraPerspective forcedCameraPerspective() {
        return this.cameraPerspective;
    }

    @Override
    public void sendCameraFade(@NonNull CameraFade fade) {
        Objects.requireNonNull(fade, "fade cannot be null!");
        CameraFadeInstruction fadeInstruction = new CameraFadeInstruction();
        fadeInstruction.setColor(fade.color());
        fadeInstruction.setTimeData(
                new CameraFadeInstruction.TimeData(
                        fade.fadeInSeconds(),
                        fade.fadeHoldSeconds(),
                        fade.fadeOutSeconds()
                )
        );

        CameraInstructionPacket packet = new CameraInstructionPacket();
        packet.setFadeInstruction(fadeInstruction);
        session.sendUpstreamPacket(packet);
    }

    @Override
    public void sendCameraPosition(@NonNull CameraPosition movement) {
        Objects.requireNonNull(movement, "movement cannot be null!");
        this.cameraPerspective = CameraPerspective.FREE; // Movements only work with the free preset
        CameraSetInstruction setInstruction = new CameraSetInstruction();

        CameraEaseType easeType = movement.easeType();
        if (easeType != null) {
            setInstruction.setEase(new CameraSetInstruction.EaseData(
                    CameraEase.fromName(easeType.id()),
                    movement.easeSeconds()
            ));
        }

        Vector3f facingPosition = movement.facingPosition();
        if (facingPosition != null) {
            setInstruction.setFacing(facingPosition);
        }

        setInstruction.setPos(movement.position());
        setInstruction.setRot(Vector2f.from(movement.rotationX(), movement.rotationY()));
        setInstruction.setPreset(CameraDefinitions.getByFunctionality(movement.playerPositionForAudio(), movement.renderPlayerEffects()));

        CameraInstructionPacket packet = new CameraInstructionPacket();
        packet.setSetInstruction(setInstruction);

        // If present, also send the fade
        CameraFade fade = movement.cameraFade();
        if (fade != null) {
            CameraFadeInstruction fadeInstruction = new CameraFadeInstruction();
            fadeInstruction.setColor(fade.color());
            fadeInstruction.setTimeData(
                    new CameraFadeInstruction.TimeData(
                            fade.fadeInSeconds(),
                            fade.fadeHoldSeconds(),
                            fade.fadeOutSeconds()
                    )
            );
            packet.setFadeInstruction(fadeInstruction);
        }
        session.sendUpstreamPacket(packet);
    }

    @Override
    public void shakeCamera(float intensity, float duration, @NonNull CameraShake type) {
        Objects.requireNonNull(type, "camera shake type must be non null!");
        CameraShakePacket packet = new CameraShakePacket();
        packet.setIntensity(intensity);
        packet.setDuration(duration);
        packet.setShakeType(type == CameraShake.POSITIONAL ? CameraShakeType.POSITIONAL : CameraShakeType.ROTATIONAL);
        packet.setShakeAction(CameraShakeAction.ADD);
        session.sendUpstreamPacket(packet);
    }

    @Override
    public void stopCameraShake() {
        CameraShakePacket packet = new CameraShakePacket();
        // CameraShakeAction.STOP removes all types regardless of the given type, but regardless it can't be null
        packet.setShakeType(CameraShakeType.POSITIONAL);
        packet.setShakeAction(CameraShakeAction.STOP);
        session.sendUpstreamPacket(packet);
    }

    @Override
    public void sendFog(String... fogNameSpaces) {
        Collections.addAll(this.appliedFog, fogNameSpaces);

        PlayerFogPacket packet = new PlayerFogPacket();
        packet.getFogStack().addAll(this.appliedFog);
        session.sendUpstreamPacket(packet);
    }

    @Override
    public void removeFog(String... fogNameSpaces) {
        if (fogNameSpaces.length == 0) {
            this.appliedFog.clear();
        } else {
            for (String id : fogNameSpaces) {
                this.appliedFog.remove(id);
            }
        }
        PlayerFogPacket packet = new PlayerFogPacket();
        packet.getFogStack().addAll(this.appliedFog);
        session.sendUpstreamPacket(packet);
    }

    @Override
    public @NonNull Set<String> fogEffects() {
        // Use a copy so that sendFog/removeFog can be called while iterating the returned set (avoid CME)
        return Set.copyOf(this.appliedFog);
    }

    @Override
    public boolean lockCamera(boolean lock, @NonNull UUID owner) {
        Objects.requireNonNull(owner, "owner cannot be null!");
        if (lock) {
            this.cameraLockOwners.add(owner);
        } else {
            this.cameraLockOwners.remove(owner);
        }

        session.lockInputs(isCameraLocked(), session.entities().isMovementLocked());
        return isCameraLocked();
    }

    @Override
    public boolean isCameraLocked() {
        return !this.cameraLockOwners.isEmpty();
    }
}