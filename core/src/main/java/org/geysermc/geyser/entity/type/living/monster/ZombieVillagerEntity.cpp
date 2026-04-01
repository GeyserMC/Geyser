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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.merchant.VillagerEntity"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.VillagerData"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class ZombieVillagerEntity extends ZombieEntity {

    public ZombieVillagerEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setTransforming(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.IS_TRANSFORMING, entityMetadata.getPrimitiveValue());
        setFlag(EntityFlag.SHAKING, isShaking());
    }

    public void setZombieVillagerData(EntityMetadata<VillagerData, ?> entityMetadata) {
        VillagerData villagerData = entityMetadata.getValue();
        dirtyMetadata.put(EntityDataTypes.VARIANT, VillagerEntity.getBedrockProfession(villagerData.getProfession()));
        dirtyMetadata.put(EntityDataTypes.MARK_VARIANT, VillagerEntity.getBedrockRegion(villagerData.getType()));

        dirtyMetadata.put(EntityDataTypes.TRADE_TIER, villagerData.getLevel() - 1);
    }

    override protected bool isShaking() {
        return getFlag(EntityFlag.IS_TRANSFORMING) || super.isShaking();
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (itemInHand.is(Items.GOLDEN_APPLE)) {
            return InteractiveTag.CURE;
        } else {
            return super.testMobInteraction(hand, itemInHand);
        }
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        if (itemInHand.is(Items.GOLDEN_APPLE)) {

            return InteractionResult.CONSUME;
        } else {
            return super.mobInteract(hand, itemInHand);
        }
    }
}
