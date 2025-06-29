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
import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.util.Identifier;

import java.util.function.Predicate;

/**
 * Geyser data components are data components used for non-vanilla items only. Like vanilla data components, they indicate behaviour of custom items, and like vanilla data components, it is expected
 * that this behaviour is also present server-side and on Java clients.
 *
 * <p>Most components in this class will likely be deprecated in the future as Mojang introduces Java counterparts.</p>
 *
 * @see DataComponent
 * @see CustomItemDefinition#components()
 */
public interface GeyserDataComponent {

    /**
     * Marks this item as chargeable, meaning an item functions as a bow or a crossbow.
     * A list of bedrock item identifiers can be given as ammunition.
     *
     * @see Chargeable
     */
    DataComponent<Chargeable> CHARGEABLE = createGeyser("chargeable");

    /**
     * Places a visual indicator (=tooltip) of the item's attack damage. Must be at or above 0.
     *
     * <p>Attribute modifiers are automatically translated for custom vanilla items, but not for non-vanilla ones, which is why this component is here.</p>
     */
    DataComponent<Integer> ATTACK_DAMAGE = createGeyser("attack_damage", i -> i >= 0);

    /**
     * Indicates which block the item should place and whether it should replace the original item for that block.
     */
    DataComponent<BlockPlacer> BLOCK_PLACER = createGeyser("block_placer");

    private static <T> DataComponent<T> createGeyser(String id) {
        return createGeyser(id, t -> true);
    }

    private static <T> DataComponent<T> createGeyser(String id, Predicate<T> predicate) {
        return GeyserApi.api().provider(DataComponent.class, Identifier.of("geysermc", id), predicate, false);
    }
}
