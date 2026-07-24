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

package org.geysermc.geyser.api.item.custom.v2.component.geyser;

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.util.Unit;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Predicate;

/**
 * Geyser data components are data components used for non-vanilla items only. Like vanilla data components, they indicate behavior of custom items, and like vanilla data components, it is expected
 * that this behavior is also present server-side and on Java clients.
 *
 * <p>Most components in this class will likely be deprecated in the future as Mojang introduces Java counterparts.</p>
 *
 * @see ItemDataComponent
 * @see CustomItemDefinition#components()
 * @since 2.9.3
 */
@ApiStatus.NonExtendable
public interface GeyserItemDataComponents {

    /**
     * Marks this item as chargeable, meaning an item functions as a bow or a crossbow.
     * A list of bedrock item identifiers can be given as ammunition.
     *
     * @see GeyserChargeable
     * @since 2.9.3
     */
    ItemDataComponent<GeyserChargeable> CHARGEABLE = createGeyser("chargeable");

    /**
     * Places a visual indicator (=tooltip) of the item's attack damage. Must be at or above 0.
     *
     * <p>Attribute modifiers are automatically translated for custom vanilla items, but not for non-vanilla ones, which is why this component is here.</p>
     * @since 2.9.3
     */
    ItemDataComponent<Integer> ATTACK_DAMAGE = createGeyser("attack_damage", i -> i >= 0);

    /**
     * Indicates which block the item should place and whether it should replace the original item for that block.
     *
     * @see GeyserBlockPlacer
     * @since 2.9.3
     */
    ItemDataComponent<GeyserBlockPlacer> BLOCK_PLACER = createGeyser("block_placer");

    /**
     * Marks the item as throwable, meaning it can be thrown continuously by holding down the use button, and also
     * allows specifying if the client should display a swing animation when the item is thrown.
     *
     * @see GeyserThrowableComponent
     * @since 2.9.3
     */
    ItemDataComponent<GeyserThrowableComponent> THROWABLE = createGeyser("throwable");

    /**
     * Marks the item as a projectile, meaning it can be used as ammunition in the chargeable component.
     *
     * @see GeyserChargeable#ammunition()
     * @since 2.9.3
     */
    ItemDataComponent<Unit> PROJECTILE = createGeyser("projectile");

    /**
     * Marks the item as an entity placer, meaning it can place entities, e.g. a boat or minecart item.
     *
     * <p>All items placing entities should be marked with this component to prevent client-side desyncs.</p>
     * @since 2.9.3
     */
    ItemDataComponent<Unit> ENTITY_PLACER = createGeyser("entity_placer");

    private static <T> ItemDataComponent<T> createGeyser(String id) {
        return createGeyser(id, t -> true);
    }

    private static <T> ItemDataComponent<T> createGeyser(String id, Predicate<T> predicate) {
        return GeyserApi.api().provider(ItemDataComponent.class, Identifier.of("geysermc", id), predicate, false);
    }
}
