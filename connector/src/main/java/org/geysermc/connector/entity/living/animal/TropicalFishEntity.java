/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.entity.living.animal;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.connector.entity.living.AbstractFishEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class TropicalFishEntity extends AbstractFishEntity {

    public TropicalFishEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 16) {
            TropicalFishVariant variant = TropicalFishVariant.fromVariantNumber((int) entityMetadata.getValue());

            metadata.put(EntityData.VARIANT, variant.getShape()); // Shape 0-1
            metadata.put(EntityData.MARK_VARIANT, variant.getPattern()); // Pattern 0-5
            metadata.put(EntityData.COLOR, variant.getBaseColor()); // Base color 0-15
            metadata.put(EntityData.COLOR_2, variant.getPatternColor()); // Pattern color 0-15
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Getter
    @AllArgsConstructor
    private static class TropicalFishVariant {
        private int shape;
        private int pattern;
        private byte baseColor;
        private byte patternColor;

        /**
         * Convert the variant number from Java into separate values
         *
         * @param varNumber Variant number from Java edition
         *
         * @return The variant converted into TropicalFishVariant
         */
        public static TropicalFishVariant fromVariantNumber(int varNumber) {
            return new TropicalFishVariant((varNumber & 0xFF), ((varNumber >> 8) & 0xFF), (byte) ((varNumber >> 16) & 0xFF), (byte) ((varNumber >> 24) & 0xFF));
        }
    }
}
