/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;

public class RabbitEntity extends AnimalEntity {

    public RabbitEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        super.updateBedrockMetadata(entityMetadata, session);
        if (entityMetadata.getId() == 16) {
            metadata.put(EntityData.SCALE, .55f);
            boolean isBaby = (boolean) entityMetadata.getValue();
            if (isBaby) {
                metadata.put(EntityData.SCALE, .35f);
                metadata.getFlags().setFlag(EntityFlag.BABY, true);
            }
        } else if (entityMetadata.getId() == 17) {
            int variant = (int) entityMetadata.getValue();

            // Change the killer bunny to display as white since it only exists on Java Edition
            boolean isKillerBunny = variant == 99;
            if (isKillerBunny) {
                variant = 1;
            }
            // Allow the resource pack to adjust to the killer bunny
            metadata.getFlags().setFlag(EntityFlag.BRIBED, isKillerBunny);

            metadata.put(EntityData.VARIANT, variant);
        }
    }

    @Override
    public boolean canEat(GeyserSession session, String javaIdentifierStripped, ItemEntry itemEntry) {
        return javaIdentifierStripped.equals("dandelion") || javaIdentifierStripped.equals("carrot") || javaIdentifierStripped.equals("golden_carrot");
    }
}