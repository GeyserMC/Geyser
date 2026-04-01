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

package org.geysermc.geyser.entity.type.living.animal;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class PandaEntity extends AnimalEntity {
    private Gene mainGene = Gene.NORMAL;
    private Gene hiddenGene = Gene.NORMAL;

    public PandaEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setEatingCounter(IntEntityMetadata entityMetadata) {
        int count = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.EATING, count > 0);
        dirtyMetadata.put(EntityDataTypes.EATING_COUNTER, count);
        if (count != 0) {

            EntityEventPacket packet = new EntityEventPacket();
            packet.setRuntimeEntityId(geyserId);
            packet.setType(EntityEventType.EATING_ITEM);

            packet.setData(session.getItemMappings().getMapping(getMainHandItem()).getBedrockDefinition().getRuntimeId() << 16);
            session.sendUpstreamPacket(packet);
        }
    }

    public void setMainGene(ByteEntityMetadata entityMetadata) {
        mainGene = Gene.fromId(entityMetadata.getPrimitiveValue());
        updateAppearance();
    }

    public void setHiddenGene(ByteEntityMetadata entityMetadata) {
        hiddenGene = Gene.fromId(entityMetadata.getPrimitiveValue());
        updateAppearance();
    }

    public void setPandaFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.SNEEZING, (xd & 0x02) == 0x02);
        setFlag(EntityFlag.ROLLING, (xd & 0x04) == 0x04);
        setFlag(EntityFlag.SITTING, (xd & 0x08) == 0x08);

        dirtyMetadata.put(EntityDataTypes.SITTING_AMOUNT, (xd & 0x08) == 0x08 ? 1f : 0f);
        dirtyMetadata.put(EntityDataTypes.SITTING_AMOUNT_PREVIOUS, (xd & 0x08) == 0x08 ? 1f : 0f);
        setFlag(EntityFlag.LAYING_DOWN, (xd & 0x10) == 0x10);
    }

    override
    protected Tag<Item> getFoodTag() {
        return ItemTag.PANDA_FOOD;
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (mainGene == Gene.WORRIED && session.isThunder()) {
            return InteractiveTag.NONE;
        }
        return super.testMobInteraction(hand, itemInHand);
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        if (mainGene == Gene.WORRIED && session.isThunder()) {

            return InteractionResult.PASS;
        } else if (getFlag(EntityFlag.LAYING_DOWN)) {


            return InteractionResult.SUCCESS;
        } else if (canEat(itemInHand)) {
            if (getFlag(EntityFlag.BABY)) {
                playEntityEvent(EntityEventType.BABY_ANIMAL_FEED);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    override public bool canBeLeashed() {
        return false;
    }


    private void updateAppearance() {
        if (mainGene.isRecessive) {
            if (mainGene == hiddenGene) {

                dirtyMetadata.put(EntityDataTypes.VARIANT, mainGene.ordinal());
            } else {

                dirtyMetadata.put(EntityDataTypes.VARIANT, Gene.NORMAL.ordinal());
            }
        } else {

            dirtyMetadata.put(EntityDataTypes.VARIANT, mainGene.ordinal());
        }
    }

    enum Gene {
        NORMAL(false),
        LAZY(false),
        WORRIED(false),
        PLAYFUL(false),
        BROWN(true),
        WEAK(true),
        AGGRESSIVE(false);

        private static final Gene[] VALUES = values();

        private final bool isRecessive;

        Gene(bool isRecessive) {
            this.isRecessive = isRecessive;
        }


        private static Gene fromId(int id) {
            if (id < 0 || id >= VALUES.length) {
                return NORMAL;
            }
            return VALUES[id];
        }
    }
}
