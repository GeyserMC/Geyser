/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity;

import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.camera.CameraEase;
import org.cloudburstmc.protocol.bedrock.data.camera.CameraSetInstruction;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.CameraInstructionPacket;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;
import org.geysermc.geyser.api.bedrock.camera.CameraPerspective;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.impl.camera.CameraDefinitions;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MathUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class EntitySpectateHelper {

    private static final int FIRST_PERSON_PRESET = CameraPerspective.FIRST_PERSON.ordinal();
    private static final int FREE_PRESET = CameraPerspective.FREE.ordinal();
    private static final double DISTANCE = 4.0;
    private static final float EYE_HEIGHT_RATIO = 0.85f;

    private EntitySpectateHelper() {
    }

    public enum SpectateMode {
        FIRST_PERSON,
        THIRD_PERSON_BACK,
        THIRD_PERSON_FRONT;

        private static final SpectateMode[] VALUES = values();

        public SpectateMode next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    public static boolean isSpectating(GeyserSession session) {
        return session.getSpectatedEntity() != null;
    }

    public static void start(GeyserSession session, Entity target) {
        Entity previous = session.getSpectatedEntity();
        session.setSpectatedEntity(target);
        session.setSpectateMode(SpectateMode.FIRST_PERSON);
        if (previous != null && previous != target) {
            previous.sendSpectateInvisible(false);
        }
        setSelfHidden(session, true);
        apply(session, target, SpectateMode.FIRST_PERSON);
    }

    public static void stop(GeyserSession session) {
        Entity target = session.getSpectatedEntity();
        if (target == null) {
            return;
        }
        session.setSpectatedEntity(null);
        session.setSpectateMode(SpectateMode.FIRST_PERSON);
        if (target.isValid()) {
            target.sendSpectateInvisible(false);
        }
        setSelfHidden(session, false);
        // Hand the camera back as the default first-person preset, then clear a few ticks later to re-enable F5. Clearing directly
        // gets ignored after a reconnect while a camera set is still in flight, but a set instruction restores the view either way.
        CameraInstructionPacket reset = new CameraInstructionPacket();
        CameraSetInstruction defaultCamera = new CameraSetInstruction();
        defaultCamera.setPreset(CameraDefinitions.getById(FIRST_PERSON_PRESET));
        defaultCamera.setDefaultPreset(OptionalBoolean.of(true));
        reset.setSetInstruction(defaultCamera);
        session.sendUpstreamPacket(reset);
        session.scheduleInEventLoop(() -> {
            CameraInstructionPacket clear = new CameraInstructionPacket();
            clear.setClear(true);
            session.sendUpstreamPacket(clear);
        }, 150, TimeUnit.MILLISECONDS);
    }

    public static void cycleMode(GeyserSession session) {
        Entity target = session.getSpectatedEntity();
        if (target == null) {
            return;
        }
        SpectateMode mode = session.getSpectateMode().next();
        session.setSpectateMode(mode);
        apply(session, target, mode);
    }

    public static void tick(GeyserSession session) {
        Entity target = session.getSpectatedEntity();
        if (target == null) {
            return;
        }
        if (!target.isValid()) {
            stop(session);
            return;
        }
        sendCamera(session, target, session.getSpectateMode());
    }

    private static void apply(GeyserSession session, Entity target, SpectateMode mode) {
        target.sendSpectateInvisible(mode == SpectateMode.FIRST_PERSON);
        sendCamera(session, target, mode);
    }

    private static void setSelfHidden(GeyserSession session, boolean hidden) {
        Entity self = session.getPlayerEntity();
        self.setFlag(EntityFlag.INVISIBLE, hidden);
        self.updateBedrockMetadata();
    }

    private static void sendCamera(GeyserSession session, Entity entity, SpectateMode mode) {
        float yaw = entity.getHeadYaw();
        float pitch = entity.getPitch();
        Vector3f forward = MathUtils.calculateViewVector(pitch, yaw);

        Vector3f pos = entity.position();
        double eyeY = pos.getY() + eyeHeight(entity);

        // First-person sits at the eye. Third-person offsets along the view vector (behind, or in front looking back)
        double distance;
        Vector2f rot;
        switch (mode) {
            case FIRST_PERSON -> {
                distance = 0.0;
                rot = Vector2f.from(pitch, yaw);
            }
            case THIRD_PERSON_BACK -> {
                distance = -DISTANCE;
                rot = Vector2f.from(pitch, yaw);
            }
            case THIRD_PERSON_FRONT -> {
                distance = DISTANCE;
                rot = Vector2f.from(-pitch, yaw + 180f);
            }
            default -> throw new IllegalStateException("Unexpected spectate mode: " + mode);
        }
        double cx = pos.getX() + forward.getX() * distance;
        double cy = eyeY + forward.getY() * distance;
        double cz = pos.getZ() + forward.getZ() * distance;

        CameraInstructionPacket packet = new CameraInstructionPacket();
        CameraSetInstruction set = new CameraSetInstruction();
        set.setPreset(CameraDefinitions.getById(FREE_PRESET));
        set.setPos(Vector3f.from(cx, cy, cz));
        set.setRot(rot);
        set.setEase(new CameraSetInstruction.EaseData(CameraEase.LINEAR, 0.15f));
        packet.setSetInstruction(set);
        session.sendUpstreamPacket(packet);
    }

    private static float eyeHeight(Entity entity) {
        return Objects.requireNonNullElse(entity.override(GeyserEntityDataTypes.HEIGHT), entity.getBoundingBoxHeight()) * EYE_HEIGHT_RATIO;
    }
}
