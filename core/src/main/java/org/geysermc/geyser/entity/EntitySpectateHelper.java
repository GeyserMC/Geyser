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
import org.geysermc.geyser.api.bedrock.camera.CameraPerspective;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.impl.camera.CameraDefinitions;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MathUtils;

public final class EntitySpectateHelper {

    private static final int FIRST_PERSON_PRESET = CameraPerspective.FIRST_PERSON.ordinal();
    private static final int FREE_PRESET = CameraPerspective.FREE.ordinal();
    private static final double DISTANCE = 4.0;
    private static final float EYE_HEIGHT_RATIO = 0.85f;

    private EntitySpectateHelper() {
    }

    public static boolean isSpectating(GeyserSession session) {
        return session.getSpectatedEntity() != null;
    }

    public static void start(GeyserSession session, Entity target) {
        Entity previous = session.getSpectatedEntity();
        session.setSpectatedEntity(target);
        session.setSpectateMode(0);
        if (previous != null && previous != target) {
            previous.sendSpectateInvisible(false);
        }
        setSelfHidden(session, true);
        apply(session, target, 0);
    }

    public static void stop(GeyserSession session) {
        Entity target = session.getSpectatedEntity();
        if (target == null) {
            return;
        }
        session.setSpectatedEntity(null);
        session.setSpectateMode(0);
        if (target.isValid()) {
            target.sendSpectateInvisible(false);
        }
        setSelfHidden(session, false);
        // A bare clear can lose to an in-flight FREE ease (rapid enter/exit); snap to first_person first, then clear
        CameraInstructionPacket reset = new CameraInstructionPacket();
        CameraSetInstruction firstPerson = new CameraSetInstruction();
        firstPerson.setPreset(CameraDefinitions.getById(FIRST_PERSON_PRESET));
        reset.setSetInstruction(firstPerson);
        session.sendUpstreamPacket(reset);

        CameraInstructionPacket clear = new CameraInstructionPacket();
        clear.setClear(true);
        session.sendUpstreamPacket(clear);
    }

    public static void cycleMode(GeyserSession session) {
        Entity target = session.getSpectatedEntity();
        if (target == null) {
            return;
        }
        int mode = (session.getSpectateMode() + 1) % 3;
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

    private static void apply(GeyserSession session, Entity target, int mode) {
        target.sendSpectateInvisible(mode == 0);
        sendCamera(session, target, mode);
    }

    private static void setSelfHidden(GeyserSession session, boolean hidden) {
        Entity self = session.getPlayerEntity();
        self.setFlag(EntityFlag.INVISIBLE, hidden);
        self.setFlag(EntityFlag.HIDDEN_WHEN_INVISIBLE, hidden);
        self.updateBedrockMetadata();
    }

    private static void sendCamera(GeyserSession session, Entity entity, int mode) {
        float yaw = entity.getHeadYaw();
        float pitch = entity.getPitch();
        Vector3f forward = MathUtils.calculateViewVector(pitch, yaw);
        double fx = forward.getX();
        double fy = forward.getY();
        double fz = forward.getZ();

        Vector3f pos = entity.bedrockPosition();
        double eyeY = pos.getY() + eyeHeight(entity);

        double cx;
        double cy;
        double cz;
        Vector2f rot;
        if (mode == 0) {
            cx = pos.getX();
            cy = eyeY;
            cz = pos.getZ();
            rot = Vector2f.from(pitch, yaw);
        } else if (mode == 1) {
            cx = pos.getX() - fx * DISTANCE;
            cy = eyeY - fy * DISTANCE;
            cz = pos.getZ() - fz * DISTANCE;
            rot = Vector2f.from(pitch, yaw);
        } else {
            cx = pos.getX() + fx * DISTANCE;
            cy = eyeY + fy * DISTANCE;
            cz = pos.getZ() + fz * DISTANCE;
            rot = Vector2f.from(-pitch, yaw + 180f);
        }

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
        return entity.getDefinition().height() * EYE_HEIGHT_RATIO;
    }
}
