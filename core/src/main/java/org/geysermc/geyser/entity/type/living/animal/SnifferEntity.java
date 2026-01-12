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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.SnifferState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;

public class SnifferEntity extends AnimalEntity implements Tickable {
    private static final float DIGGING_HEIGHT = EntityDefinitions.SNIFFER.height() - 0.4f;
    private static final int DIG_END = 120;
    private static final int DIG_START = DIG_END - 34;

    private Pose pose = Pose.STANDING; // Needed to call setDimensions for DIGGING state
    private int digTicks;

    public SnifferEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    public void setPose(Pose pose) {
        this.pose = pose;
        super.setPose(pose);
    }

    @Override
    protected void setDimensionsFromPose(Pose pose) {
        if (getFlag(EntityFlag.DIGGING)) {
            setBoundingBoxHeight(DIGGING_HEIGHT);
            setBoundingBoxWidth(definition.width());
        } else {
            super.setDimensionsFromPose(pose);
        }
    }

    @Override
    @Nullable
    protected Tag<Item> getFoodTag() {
        return ItemTag.SNIFFER_FOOD;
    }

    public void setSnifferState(ObjectEntityMetadata<SnifferState> entityMetadata) {
        SnifferState snifferState = entityMetadata.getValue();

        // SnifferState.SCENTING and SnifferState.IDLING not used in bedrock
        // The bedrock client does the scenting animation and sound on its own
        setFlag(EntityFlag.FEELING_HAPPY, snifferState == SnifferState.FEELING_HAPPY);
        setFlag(EntityFlag.SCENTING, snifferState == SnifferState.SNIFFING); // SnifferState.SNIFFING -> EntityFlag.SCENTING
        setFlag(EntityFlag.SEARCHING, snifferState == SnifferState.SEARCHING);
        setFlag(EntityFlag.DIGGING, snifferState == SnifferState.DIGGING);
        setFlag(EntityFlag.RISING, snifferState == SnifferState.RISING);

        setDimensionsFromPose(pose);

        if (getFlag(EntityFlag.DIGGING)) {
            digTicks = DIG_END;
        } else {
            // Handles situations where the DIGGING state is exited earlier than expected,
            // such as hitting the sniffer or joining the game while it is digging
            digTicks = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        // The java client renders digging particles on its own, but bedrock does not
        if (digTicks > 0 && --digTicks < DIG_START && digTicks % 5 == 0) {
            Vector3f rot = Vector3f.createDirectionDeg(0, -getYaw()).mul(2.25f);
            Vector3f pos = getPosition().add(rot).up(0.2f).floor(); // Handle non-full blocks
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
