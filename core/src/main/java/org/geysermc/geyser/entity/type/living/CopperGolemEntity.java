/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.CopperGolemState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.WeatheringCopperState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import java.util.UUID;

public class CopperGolemEntity extends GolemEntity {
    public static final String CHEST_INTERACTION = "minecraft:chest_interaction";
    public static final String HAS_FLOWER = "has_flower";
    public static final String OXIDIZATION_LEVEL = "minecraft:oxidation_level";

    public CopperGolemEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void addAdditionalSpawnData(AddEntityPacket addEntityPacket) {
        propertyManager.add(CHEST_INTERACTION, "none");
        propertyManager.add(HAS_FLOWER, false);
        propertyManager.add(OXIDIZATION_LEVEL, "unoxidized");
    }

    @Override
    protected @NonNull InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (itemInHand.isEmpty() && !getMainHandItem().isEmpty()) {
            return InteractiveTag.DROP_ITEM;
        } else if (itemInHand.is(Items.SHEARS) && canBeSheared()) {
            return InteractiveTag.SHEAR;
        } else if (itemInHand.is(Items.HONEYCOMB)) {
            return InteractiveTag.WAX_ON;
        } else if (itemInHand.is(session, ItemTag.AXES)) {
            // There is no way of knowing if the copper golem is waxed or not,
            // so just always send a scrape tag :(
            return InteractiveTag.SCRAPE;
        }

        return super.testMobInteraction(hand, itemInHand);
    }

    @Override
    protected @NonNull InteractionResult mobInteract(@NonNull Hand usedHand, @NonNull GeyserItemStack itemInHand) {
        if ((itemInHand.isEmpty() && !getMainHandItem().isEmpty()) || (itemInHand.is(Items.SHEARS) && canBeSheared())) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private boolean canBeSheared() {
        return isAlive() && getItemInSlot(EquipmentSlot.HELMET).is(session, ItemTag.SHEARABLE_FROM_COPPER_GOLEM);
    }

    @Override
    public void setSaddle(GeyserItemStack stack) {
        super.setSaddle(stack);

        // Equipment on Java, entity property on bedrock
        propertyManager.add(HAS_FLOWER, stack.is(Items.POPPY));
        updateBedrockEntityProperties();
    }

    public void setWeatheringState(EntityMetadata<WeatheringCopperState, ? extends MetadataType<WeatheringCopperState>> metadata) {
        WeatheringCopperState state = metadata.getValue();
        propertyManager.add(OXIDIZATION_LEVEL, switch (state) {
            case UNAFFECTED -> "unoxidized";
            case EXPOSED -> "exposed";
            case WEATHERED -> "weathered";
            case OXIDIZED -> "oxidized";
        });
        updateBedrockEntityProperties();
    }

    public void setGolemState(EntityMetadata<CopperGolemState, ? extends MetadataType<CopperGolemState>> metadata) {
        CopperGolemState state = metadata.getValue();
        propertyManager.add(CHEST_INTERACTION, switch (state) {
            case IDLE -> "none";
            case GETTING_ITEM -> "take";
            case GETTING_NO_ITEM -> "take_fail";
            case DROPPING_ITEM -> "put";
            case DROPPING_NO_ITEM -> "put_fail";
        });
        updateBedrockEntityProperties();
    }
}
