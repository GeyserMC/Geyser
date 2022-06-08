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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.VillagerData;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.living.merchant.VillagerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;

import javax.annotation.Nonnull;
import java.util.UUID;

public class ZombieVillagerEntity extends ZombieEntity {

    public ZombieVillagerEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setTransforming(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.IS_TRANSFORMING, entityMetadata.getPrimitiveValue());
        setFlag(EntityFlag.SHAKING, isShaking());
    }

    public void setZombieVillagerData(EntityMetadata<VillagerData, ?> entityMetadata) {
        VillagerData villagerData = entityMetadata.getValue();
        dirtyMetadata.put(EntityData.VARIANT, VillagerEntity.getBedrockProfession(villagerData.getProfession())); // Actually works properly with the OptionalPack
        dirtyMetadata.put(EntityData.MARK_VARIANT, VillagerEntity.getBedrockRegion(villagerData.getType()));
        // Used with the OptionalPack
        dirtyMetadata.put(EntityData.TRADE_TIER, villagerData.getLevel() - 1);
    }

    @Override
    protected boolean isShaking() {
        return getFlag(EntityFlag.IS_TRANSFORMING) || super.isShaking();
    }

    @Nonnull
    @Override
    protected InteractiveTag testMobInteraction(Hand hand, @Nonnull GeyserItemStack itemInHand) {
        if (itemInHand.getJavaId() == session.getItemMappings().getStoredItems().goldenApple()) {
            return InteractiveTag.CURE;
        } else {
            return super.testMobInteraction(hand, itemInHand);
        }
    }

    @Nonnull
    @Override
    protected InteractionResult mobInteract(Hand hand, @Nonnull GeyserItemStack itemInHand) {
        if (itemInHand.getJavaId() == session.getItemMappings().getStoredItems().goldenApple()) {
            // The client doesn't know if the entity has weakness as that's not usually sent over the network
            return InteractionResult.CONSUME;
        } else {
            return super.mobInteract(hand, itemInHand);
        }
    }
}
