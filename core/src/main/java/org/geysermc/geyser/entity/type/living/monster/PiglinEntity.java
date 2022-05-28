/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;

import javax.annotation.Nonnull;
import java.util.UUID;

public class PiglinEntity extends BasePiglinEntity {

    public PiglinEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setBaby(BooleanEntityMetadata entityMetadata) {
        boolean isBaby = entityMetadata.getPrimitiveValue();
        dirtyMetadata.put(EntityData.SCALE, isBaby? .55f : 1f);
        setFlag(EntityFlag.BABY, isBaby);

        updateMountOffset();
    }

    public void setChargingCrossbow(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.CHARGING, entityMetadata.getPrimitiveValue());
    }

    public void setDancing(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.DANCING, entityMetadata.getPrimitiveValue());
    }

    @Override
    public void updateOffHand(GeyserSession session) {
        // Check if the Piglin is holding Gold and set the ADMIRING flag accordingly so its pose updates
        setFlag(EntityFlag.ADMIRING, session.getTagCache().shouldPiglinAdmire(session.getItemMappings().getMapping(this.offHand)));
        super.updateBedrockMetadata();

        super.updateOffHand(session);
    }

    @Nonnull
    @Override
    protected InteractiveTag testMobInteraction(@Nonnull GeyserItemStack itemInHand) {
        InteractiveTag tag = super.testMobInteraction(itemInHand);
        if (tag != InteractiveTag.NONE) {
            return tag;
        } else {
            return canGiveGoldTo(itemInHand) ? InteractiveTag.BARTER : InteractiveTag.NONE;
        }
    }

    @Nonnull
    @Override
    protected InteractionResult mobInteract(@Nonnull GeyserItemStack itemInHand) {
        InteractionResult superResult = super.mobInteract(itemInHand);
        if (superResult.consumesAction()) {
            return superResult;
        } else {
            return canGiveGoldTo(itemInHand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
    }

    private boolean canGiveGoldTo(@Nonnull GeyserItemStack itemInHand) {
        return !getFlag(EntityFlag.BABY) && itemInHand.getJavaId() == session.getItemMappings().getStoredItems().goldIngot() && !getFlag(EntityFlag.ADMIRING);
    }
}
