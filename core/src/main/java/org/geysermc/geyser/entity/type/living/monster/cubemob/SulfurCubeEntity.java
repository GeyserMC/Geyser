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
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.entity.properties.type.StringEnumProperty;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SulfurCubeEntity extends AbstractCubeEntity {

    private static final Map<Tag<Item>, String> ARCHETYPE_MAPPING = new HashMap<>();
    private static final List<String> BEDROCK_ARCHETYPES = new ArrayList<>();

    public static StringEnumProperty SULFUR_CUBE_ARCHETYPE_PROPERTY = new StringEnumProperty(
        Identifier.of("minecraft", "sulfur_cube_archetype"), BEDROCK_ARCHETYPES, 0
    );

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
        levelSoundEventPacket.setIdentifier(definition().identifier().toString());
        levelSoundEventPacket.setEntityUniqueId(this.geyserId());

        session.sendUpstreamPacket(levelSoundEventPacket);

        if (!definition().properties().contains(SULFUR_CUBE_ARCHETYPE_PROPERTY)) {
            return;
        }

        if (stack.isEmpty()) {
            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, "none");
        } else {
            String archetype = null;
            for (Map.Entry<Tag<Item>, String> entry : ARCHETYPE_MAPPING.entrySet()) {
                if (stack.is(session, entry.getKey())) {
                    archetype = entry.getValue();
                    break;
                }
            }

            if (archetype == null) archetype = "regular"; // Needs a value, this is the best default

            this.updateProperty(SULFUR_CUBE_ARCHETYPE_PROPERTY, archetype);
        }
    }

    @Override
    public void setCubeScale(IntEntityMetadata entityMetadata) {
        // Ignore, sulfur cubes use age and baby tags instead
    }

    /**
     * Slime geometry is far smaller than the sulfur cube and has no client
     * side baby scaling, so the slime fallback sizes explicitly: the medium
     * slime (scale 2.1, the cube family formula 0.10 + java size 2) is the
     * closest match for the adult's 0.98 block frame, the small slime
     * (scale 1.1) for babies. The real actor path keeps scale 1 and lets
     * the client handle sizes.
     */
    private boolean isSlimeFallback() {
        return definition() == VanillaEntities.SLIME.defaultBedrockDefinition();
    }

    @Override
    protected float getAdultSize() {
        return isSlimeFallback() ? 2.1f : 1f;
    }

    @Override
    protected float getBabySize() {
        return isSlimeFallback() ? 1.1f : 1f;
    }

    @Override
    public void setBaby(BooleanEntityMetadata entityMetadata) {
        super.setBaby(entityMetadata);
        if (isSlimeFallback()) {
            // The scaled up slime is visual compensation only; the hitbox
            // stays at the real mob's dimensions, matching the actor path.
            setBoundingBoxHeight(javaDefinition.height());
            setBoundingBoxWidth(javaDefinition.width());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fuseTickTime > 0) {
            this.fuseTickTime--;
        }

        if (this.previousFuseTickTime != this.fuseTickTime) {
            this.metadata.put(EntityDataTypes.FUSE_TIME, fuseTickTime);
            this.updateBedrockMetadata();
            this.previousFuseTickTime = this.fuseTickTime;
        }
    }

    public void setMaxFuse(IntEntityMetadata entityMetadata) {
        this.fuseTickTime = entityMetadata.getPrimitiveValue();
        this.metadata.put(EntityDataTypes.FUSE_TIME, fuseTickTime);
    }

    static {
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_BOUNCY, "bouncy");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_REGULAR, "regular");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_SLOW_BOUNCY, "slow_bouncy");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_SLOW_FLAT, "slow_flat");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_FAST_FLAT, "fast_flat");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_LIGHT, "light");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_FAST_SLIDING, "fast_sliding");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_SLOW_SLIDING, "slow_sliding");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_STICKY, "sticky");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_HIGH_RESISTANCE, "high_resistance");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE, "explosive");
        ARCHETYPE_MAPPING.put(ItemTag.SULFUR_CUBE_ARCHETYPE_HOT, "hot");

        BEDROCK_ARCHETYPES.add("none");
        BEDROCK_ARCHETYPES.addAll(ARCHETYPE_MAPPING.values());
    }
}
