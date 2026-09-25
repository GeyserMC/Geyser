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

package org.geysermc.geyser.entity.type;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.animal.TemperatureVariantAnimal;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.Optional;

@Getter
public class ThrowableEggEntity extends ThrowableItemEntity {

    // Used for egg break particles
    private GeyserItemStack itemStack;

    public ThrowableEggEntity(EntitySpawnContext context) {
        super(context);
        itemStack = GeyserItemStack.of(session, Items.EGG.javaId(), 1);
    }

    @Override
    public void setItem(EntityMetadata<ItemStack, ?> entityMetadata) {
        GeyserItemStack stack = GeyserItemStack.from(session, entityMetadata.getValue());
        TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY.apply(propertyManager, getVariantOrFallback(session, stack));
        updateBedrockEntityProperties();
        this.itemStack = stack;
    }

    private static TemperatureVariantAnimal.BuiltInVariant getVariantOrFallback(GeyserSession session, GeyserItemStack stack) {
        Integer id = stack.getComponent(DataComponentTypes.CHICKEN_VARIANT);
        if (id != null) {
            Optional<Key> chickenVariant = JavaRegistries.CHICKEN_VARIANT.getKey(session, id);
            if (chickenVariant.isPresent()) {
                for (var variant : TemperatureVariantAnimal.BuiltInVariant.values()) {
                    if (chickenVariant.get().asMinimalString().equalsIgnoreCase(variant.name())) {
                        return variant;
                    }
                }
            }
        }

        return TemperatureVariantAnimal.BuiltInVariant.TEMPERATE;
    }
}
