/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.LevelEvent"
#include "org.cloudburstmc.protocol.bedrock.data.SoundEvent"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.Tickable"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.SnifferState"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata"

public class SnifferEntity extends AnimalEntity implements Tickable {
    private static final float DIGGING_HEIGHT = EntityDefinitions.SNIFFER.height() - 0.4f;
    private static final int DIG_END = 120;
    private static final int DIG_START = DIG_END - 34;

    private Pose pose = Pose.STANDING;
    private int digTicks;

    public SnifferEntity(EntitySpawnContext context) {
        super(context);
    }

    override public void setPose(Pose pose) {
        this.pose = pose;
        super.setPose(pose);
    }

    override protected void setDimensionsFromPose(Pose pose) {
        if (getFlag(EntityFlag.DIGGING)) {
            setBoundingBoxHeight(DIGGING_HEIGHT);
            setBoundingBoxWidth(definition.width());
        } else {
            super.setDimensionsFromPose(pose);
        }
    }

    override
    protected Tag<Item> getFoodTag() {
        return ItemTag.SNIFFER_FOOD;
    }

    public void setSnifferState(ObjectEntityMetadata<SnifferState> entityMetadata) {
        SnifferState snifferState = entityMetadata.getValue();



        setFlag(EntityFlag.FEELING_HAPPY, snifferState == SnifferState.FEELING_HAPPY);
        setFlag(EntityFlag.SCENTING, snifferState == SnifferState.SNIFFING);
        setFlag(EntityFlag.SEARCHING, snifferState == SnifferState.SEARCHING);
        setFlag(EntityFlag.DIGGING, snifferState == SnifferState.DIGGING);
        setFlag(EntityFlag.RISING, snifferState == SnifferState.RISING);

        setDimensionsFromPose(pose);

        if (getFlag(EntityFlag.DIGGING)) {
            digTicks = DIG_END;
        } else {


            digTicks = 0;
        }
    }

    override public void tick() {
        super.tick();

        if (digTicks > 0 && --digTicks < DIG_START && digTicks % 5 == 0) {
            Vector3f rot = Vector3f.createDirectionDeg(0, -getYaw()).mul(2.25f);
            Vector3f pos = bedrockPosition().add(rot).up(0.2f).floor();
            int blockId = session.getBlockMappings().getBedrockBlockId(session.getGeyser().getWorldManager().getBlockAt(session, pos.toInt().down()));

            LevelEventPacket levelEventPacket = new LevelEventPacket();
            levelEventPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK_NO_SOUND);
            levelEventPacket.setPosition(pos);
            levelEventPacket.setData(blockId);
            session.sendUpstreamPacket(levelEventPacket);

            if (digTicks % 10 == 0) {
                LevelSoundEventPacket levelSoundEventPacket = new LevelSoundEventPacket();
                levelSoundEventPacket.setSound(SoundEvent.HIT);
                levelSoundEventPacket.setPosition(pos);
                levelSoundEventPacket.setExtraData(blockId);
                levelSoundEventPacket.setIdentifier(":");
                session.sendUpstreamPacket(levelSoundEventPacket);
            }
        }
    }
}
