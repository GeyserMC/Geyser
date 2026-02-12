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

package org.geysermc.geyser.entity.type;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;

import java.util.Objects;

public class DisplayBaseEntity extends Entity {

    private @NonNull Vector3f baseTranslation = Vector3f.ZERO;

    public DisplayBaseEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setTranslation(EntityMetadata<Vector3f, ?> translationMeta) {
        Vector3f oldTranslation = this.baseTranslation;
        this.baseTranslation = translationMeta.getValue() == null ? Vector3f.ZERO : translationMeta.getValue();

        // If translations are the same, don't update
        if (Objects.equals(oldTranslation, this.baseTranslation)) {
            return;
        }

        // No more translation, remove the old
        if (Vector3f.ZERO.equals(this.baseTranslation)) {
            if (vehicle == null) {
                this.setRiderSeatPosition(this.baseTranslation);
                this.moveRelativeRaw(-oldTranslation.getX(), -oldTranslation.getY(), -oldTranslation.getZ(), yaw, pitch, headYaw, false);
            } else {
                EntityUtils.updateMountOffset(this, this.vehicle, true, true, 0, 1);
                this.updateBedrockMetadata();
            }
            return;
        }

        // Use the diff between the old and new translation
        float xTranslation = oldTranslation.getX() - this.baseTranslation.getX();
        float yTranslation = oldTranslation.getY() - this.baseTranslation.getY();
        float zTranslation = oldTranslation.getZ() - this.baseTranslation.getZ();

        if (this.vehicle == null) {
            this.setRiderSeatPosition(this.baseTranslation);
            this.moveRelativeRaw(xTranslation, yTranslation, zTranslation, yaw, pitch, headYaw, false);
        } else {
            EntityUtils.updateMountOffset(this, this.vehicle, true, true, 0, 1);
            this.updateBedrockMetadata();
        }
    }

    public Vector3f getTranslation() {
        return baseTranslation;
    }
}
