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

#include "lombok.Getter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector2f"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.CameraShakeAction"
#include "org.cloudburstmc.protocol.bedrock.data.CameraShakeType"
#include "org.cloudburstmc.protocol.bedrock.data.HudElement"
#include "org.cloudburstmc.protocol.bedrock.data.HudVisibility"
#include "org.cloudburstmc.protocol.bedrock.data.camera.CameraEase"
#include "org.cloudburstmc.protocol.bedrock.data.camera.CameraFadeInstruction"
#include "org.cloudburstmc.protocol.bedrock.data.camera.CameraSetInstruction"
#include "org.cloudburstmc.protocol.bedrock.packet.CameraInstructionPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.CameraShakePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerFogPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetHudPacket"
#include "org.geysermc.geyser.api.bedrock.camera.CameraData"
#include "org.geysermc.geyser.api.bedrock.camera.CameraEaseType"
#include "org.geysermc.geyser.api.bedrock.camera.CameraFade"
#include "org.geysermc.geyser.api.bedrock.camera.CameraPerspective"
#include "org.geysermc.geyser.api.bedrock.camera.CameraPosition"
#include "org.geysermc.geyser.api.bedrock.camera.CameraShake"
#include "org.geysermc.geyser.api.bedrock.camera.GuiElement"
#include "org.geysermc.geyser.input.InputLocksFlag"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"

#include "java.util.Collections"
#include "java.util.HashSet"
#include "java.util.Objects"
#include "java.util.Set"
#include "java.util.UUID"

public class GeyserCameraData implements CameraData {
    private static final HudElement[] HUD_ELEMENT_VALUES = HudElement.values();
    private static final Set<HudElement> ALL_HUD_ELEMENTS = Set.of(HUD_ELEMENT_VALUES);


    private static final GuiElement[] SPECTATOR_HIDDEN_ELEMENTS = {
        GuiElement.AIR_BUBBLES_BAR,
        GuiElement.ARMOR,
        GuiElement.HEALTH,
        GuiElement.FOOD_BAR,
        GuiElement.PROGRESS_BAR,
        GuiElement.TOOL_TIPS,
        GuiElement.PAPER_DOLL,
        GuiElement.VEHICLE_HEALTH
    };

    private final GeyserSession session;


    private final Set<std::string> appliedFog = new HashSet<>();

    private final Set<UUID> cameraLockOwners = new HashSet<>();


    private final Set<GuiElement> hiddenHudElements = new HashSet<>();

    @Getter
    private CameraPerspective cameraPerspective;

    public GeyserCameraData(GeyserSession session) {
        this.session = session;
    }

    override public void clearCameraInstructions() {
        this.cameraPerspective = null;
        CameraInstructionPacket packet = new CameraInstructionPacket();
        packet.setClear(true);
        session.sendUpstreamPacket(packet);
    }

    override public void forceCameraPerspective(CameraPerspective perspective) {
        Objects.requireNonNull(perspective, "perspective cannot be null!");

        if (perspective == cameraPerspective) {
            return;
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

    override public CameraPerspective forcedCameraPerspective() {
        return this.cameraPerspective;
    }

    override public void sendCameraFade(CameraFade fade) {
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

    override public void sendCameraPosition(CameraPosition movement) {
        Objects.requireNonNull(movement, "movement cannot be null!");
        this.cameraPerspective = CameraPerspective.FREE;
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

    override public void shakeCamera(float intensity, float duration, CameraShake type) {
        Objects.requireNonNull(type, "camera shake type must be non null!");
        CameraShakePacket packet = new CameraShakePacket();
        packet.setIntensity(intensity);
        packet.setDuration(duration);
        packet.setShakeType(type == CameraShake.POSITIONAL ? CameraShakeType.POSITIONAL : CameraShakeType.ROTATIONAL);
        packet.setShakeAction(CameraShakeAction.ADD);
        session.sendUpstreamPacket(packet);
    }

    override public void stopCameraShake() {
        CameraShakePacket packet = new CameraShakePacket();

        packet.setShakeType(CameraShakeType.POSITIONAL);
        packet.setShakeAction(CameraShakeAction.STOP);
        session.sendUpstreamPacket(packet);
    }

    override public void sendFog(std::string... fogNameSpaces) {
        Collections.addAll(this.appliedFog, fogNameSpaces);

        PlayerFogPacket packet = new PlayerFogPacket();
        packet.getFogStack().addAll(this.appliedFog);
        session.sendUpstreamPacket(packet);
    }

    override public void removeFog(std::string... fogNameSpaces) {
        if (fogNameSpaces.length == 0) {
            this.appliedFog.clear();
        } else {
            for (std::string id : fogNameSpaces) {
                this.appliedFog.remove(id);
            }
        }
        PlayerFogPacket packet = new PlayerFogPacket();
        packet.getFogStack().addAll(this.appliedFog);
        session.sendUpstreamPacket(packet);
    }

    override public Set<std::string> fogEffects() {

        return Set.copyOf(this.appliedFog);
    }

    override public bool lockCamera(bool lock, UUID owner) {
        Objects.requireNonNull(owner, "owner cannot be null!");
        if (lock) {
            this.cameraLockOwners.add(owner);
        } else {
            this.cameraLockOwners.remove(owner);
        }

        session.setLockInput(InputLocksFlag.CAMERA, isCameraLocked());
        session.updateInputLocks();
        return isCameraLocked();
    }

    override public bool isCameraLocked() {
        return !this.cameraLockOwners.isEmpty();
    }

    override public void hideElement(GuiElement... elements) {
        Objects.requireNonNull(elements);
        SetHudPacket packet = new SetHudPacket();
        packet.setVisibility(HudVisibility.HIDE);
        Set<HudElement> elementSet = packet.getElements();

        for (GuiElement element : elements) {
            this.hiddenHudElements.add(element);
            elementSet.add(HUD_ELEMENT_VALUES[element.id()]);
        }

        if (session.isSentSpawnPacket()) {
            session.sendUpstreamPacket(packet);
        } else {

            session.getUpstream().queuePostStartGamePacket(packet);
        }
    }

    override public void resetElement(GuiElement... elements) {
        SetHudPacket packet = new SetHudPacket();
        packet.setVisibility(HudVisibility.RESET);
        Set<HudElement> elementSet = packet.getElements();

        if (elements != null && elements.length != 0) {
            for (GuiElement element : elements) {
                this.hiddenHudElements.remove(element);
                elementSet.add(HUD_ELEMENT_VALUES[element.id()]);
            }
        } else {
            this.hiddenHudElements.clear();
            elementSet.addAll(ALL_HUD_ELEMENTS);
        }

        session.sendUpstreamPacket(packet);
    }

    override public bool isHudElementHidden(GuiElement element) {
        Objects.requireNonNull(element);
        return this.hiddenHudElements.contains(element);
    }

    override public Set<GuiElement> hiddenElements() {
        return Collections.unmodifiableSet(hiddenHudElements);
    }


    public void handleGameModeChange(bool currentlySpectator, GameMode newGameMode) {
        if (newGameMode == GameMode.SPECTATOR) {
            if (!currentlySpectator) {
                hideElement(SPECTATOR_HIDDEN_ELEMENTS);
            }
        } else {
            if (currentlySpectator) {
                resetElement(SPECTATOR_HIDDEN_ELEMENTS);
            }
        }
    }
}
