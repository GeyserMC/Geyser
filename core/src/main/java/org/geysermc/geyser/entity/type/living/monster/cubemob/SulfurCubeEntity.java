/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.monster.cubemob;

import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.properties.type.StringEnumProperty;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

import java.util.List;

public class SulfurCubeEntity extends AbstractCubeEntity {

    public static StringEnumProperty SULFUR_CUBE_ARCHETYPE_PROPERTY = new StringEnumProperty(Identifier.of("minecraft", "sulfur_cube_archetype"), List.of(
        "none",
        "bouncy",
        "regular",
        "slow_bouncy",
        "slow_flat",
        "fast_flat",
        "light",
        "fast_sliding",
        "slow_sliding",
        "sticky",
        "high_resistance",
        "explosive",
        "hot"
    ), 0);

    private int previousFuseTickTime = -1;
    private int fuseTickTime = -1;

    public SulfurCubeEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    public void setBody(GeyserItemStack stack) {
        MobEquipmentPacket handPacket = new MobEquipmentPacket();
        handPacket.setRuntimeEntityId(geyserId);
        handPacket.setItem(ItemTranslator.translateToBedrock(session, stack));
        handPacket.setHotbarSlot(0);
        handPacket.setInventorySlot(0);
        handPacket.setContainerId(ContainerId.INVENTORY);

        session.sendUpstreamPacket(handPacket);

        LevelSoundEventPacket levelSoundEventPacket = new LevelSoundEventPacket();

        if (stack.isEmpty()) {
            levelSoundEventPacket.setSound(SoundEvent.EJECT_BLOCK);
        } else {
            levelSoundEventPacket.setSound(SoundEvent.ABSORB_BLOCK);
        }

        levelSoundEventPacket.setPosition(bedrockPosition());
        levelSoundEventPacket.setExtraData(-1);
        levelSoundEventPacket.setIdentifier("minecraft:sulfur_cube");
        levelSoundEventPacket.setEntityUniqueId(this.geyserId());

        session.sendUpstreamPacket(levelSoundEventPacket);

        if (stack.isEmpty()) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "none");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_BOUNCY)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "bouncy");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_SLOW_BOUNCY)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "slow_bouncy");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_SLOW_FLAT)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "slow_flat");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_FAST_FLAT)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "fast_flat");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_LIGHT)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "light");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_FAST_SLIDING)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "fast_sliding");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_SLOW_SLIDING)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "slow_sliding");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_STICKY)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "sticky");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_HIGH_RESISTANCE)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "high_resistance");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "explosive");
        } else if (stack.is(session, ItemTag.SULFUR_CUBE_ARCHETYPE_HOT)) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "hot");
        } else { // If it has a block, hide the inner texture, no matter if it's really regular
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "regular");
        }
    }

    @Override
    public void setCubeScale(IntEntityMetadata entityMetadata) {
        // Ignore, sulfur cubes use age and baby tags instead
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fuseTickTime > 0) {
            this.fuseTickTime--;
        }

        if (this.previousFuseTickTime != this.fuseTickTime) {
            this.dirtyMetadata.put(EntityDataTypes.FUSE_TIME, fuseTickTime);
            this.updateBedrockMetadata();
            this.previousFuseTickTime = this.fuseTickTime;
        }
    }

    public void setMaxFuse(IntEntityMetadata entityMetadata) {
        this.fuseTickTime = entityMetadata.getPrimitiveValue();
        this.dirtyMetadata.put(EntityDataTypes.FUSE_TIME, fuseTickTime);
    }
}
