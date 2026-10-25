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

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;

import java.util.Objects;

public class DisplayBaseEntity extends Entity {

    @Getter
    private @NonNull Vector3f baseTranslation = Vector3f.ZERO;

    @Getter
    private @NonNull Vector3f baseScale = Vector3f.ONE;

    @Getter @Setter
    private @NonNull Quaternionf leftRotation = Quaternionf.ZERO;

    @Getter @Setter
    private @NonNull Quaternionf rightRotation = Quaternionf.ZERO;

    @Getter
    private @NonNull Billboard billboard = Billboard.FIXED;

    @Getter
    private float viewDistance = -1;

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

        if (this.vehicle == null) {
            this.setRiderSeatPosition(this.baseTranslation);
            this.moveAbsoluteRaw(position, yaw, pitch, headYaw, onGround, true);
        } else {
            EntityUtils.updateMountOffset(this, this.vehicle, true, true, 0, 1);
            this.updateBedrockMetadata();
        }
    }

    public void setScale(EntityMetadata<Vector3f, ?> translationMeta) {
        this.baseScale = translationMeta.getValue();
    }

    public void setBillboard(ByteEntityMetadata metadata) {
        this.billboard = Billboard.values()[metadata.getPrimitiveValue()];
    }

    public void setViewDistance(FloatEntityMetadata metadata) {
        this.viewDistance = metadata.getPrimitiveValue();
    }

    @Override
    public Vector3f bedrockPosition() {
        return super.bedrockPosition().add(baseTranslation);
    }

    public enum Billboard {
        FIXED,
        VERTICAL,
        HORIZONTAL,
        CENTER;
    }
}
