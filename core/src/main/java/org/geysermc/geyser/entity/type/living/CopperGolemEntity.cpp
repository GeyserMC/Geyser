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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.entity.properties.type.BooleanProperty"
#include "org.geysermc.geyser.entity.properties.type.EnumProperty"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.impl.IdentifierImpl"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.CopperGolemState"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.WeatheringCopperState"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class CopperGolemEntity extends GolemEntity {
    public static final BooleanProperty HAS_FLOWER_PROPERTY = new BooleanProperty(
        IdentifierImpl.of("has_flower"),
        false
    );

    public static final EnumProperty<ChestInteractionState> CHEST_INTERACTION_PROPERTY = new EnumProperty<>(
        IdentifierImpl.of("chest_interaction"),
        ChestInteractionState.class,
        ChestInteractionState.NONE
    );

    public static final EnumProperty<OxidationLevelState> OXIDATION_LEVEL_STATE_ENUM_PROPERTY = new EnumProperty<>(
        IdentifierImpl.of("oxidation_level"),
        OxidationLevelState.class,
        OxidationLevelState.UNOXIDIZED
    );

    public enum ChestInteractionState {
        NONE,
        TAKE,
        TAKE_FAIL,
        PUT,
        PUT_FAIL
    }

    public enum OxidationLevelState {
        UNOXIDIZED,
        EXPOSED,
        WEATHERED,
        OXIDIZED
    }

    public CopperGolemEntity(EntitySpawnContext context) {
        super(context);
    }

    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (itemInHand.isEmpty() && !getMainHandItem().isEmpty()) {
            return InteractiveTag.DROP_ITEM;
        } else if (itemInHand.is(Items.SHEARS) && canBeSheared()) {
            return InteractiveTag.SHEAR;
        } else if (itemInHand.is(Items.HONEYCOMB)) {
            return InteractiveTag.WAX_ON;
        } else if (itemInHand.is(session, ItemTag.AXES)) {


            return InteractiveTag.SCRAPE;
        }

        return super.testMobInteraction(hand, itemInHand);
    }

    override protected InteractionResult mobInteract(Hand usedHand, GeyserItemStack itemInHand) {
        if ((itemInHand.isEmpty() && !getMainHandItem().isEmpty()) || (itemInHand.is(Items.SHEARS) && canBeSheared())) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private bool canBeSheared() {
        return isAlive() && getItemInSlot(EquipmentSlot.HELMET).is(session, ItemTag.SHEARABLE_FROM_COPPER_GOLEM);
    }

    override public void setSaddle(GeyserItemStack stack) {
        super.setSaddle(stack);


        HAS_FLOWER_PROPERTY.apply(propertyManager, stack.is(Items.POPPY));
        updateBedrockEntityProperties();
    }

    public void setWeatheringState(EntityMetadata<WeatheringCopperState, ? extends MetadataType<WeatheringCopperState>> metadata) {
        WeatheringCopperState state = metadata.getValue();
        OXIDATION_LEVEL_STATE_ENUM_PROPERTY.apply(propertyManager, switch (state) {
            case UNAFFECTED -> OxidationLevelState.UNOXIDIZED;
            case EXPOSED -> OxidationLevelState.EXPOSED;
            case WEATHERED -> OxidationLevelState.WEATHERED;
            case OXIDIZED -> OxidationLevelState.OXIDIZED;
        });
        updateBedrockEntityProperties();
    }

    public void setGolemState(EntityMetadata<CopperGolemState, ? extends MetadataType<CopperGolemState>> metadata) {
        CopperGolemState state = metadata.getValue();
        CHEST_INTERACTION_PROPERTY.apply(propertyManager, switch (state) {
            case IDLE -> ChestInteractionState.NONE;
            case GETTING_ITEM -> ChestInteractionState.TAKE;
            case GETTING_NO_ITEM -> ChestInteractionState.TAKE_FAIL;
            case DROPPING_ITEM -> ChestInteractionState.PUT;
            case DROPPING_NO_ITEM -> ChestInteractionState.PUT_FAIL;
        });
        updateBedrockEntityProperties();
    }
}
