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
import org.geysermc.geyser.entity.properties.type.BooleanProperty;
import org.geysermc.geyser.entity.properties.type.EnumProperty;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.CopperGolemState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.WeatheringCopperState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

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
