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

package org.geysermc.geyser.api.item.custom.v2.component;

import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;

/**
 * Geyser data components are data components used for non-vanilla items only. Like vanilla data components, they indicate behaviour of custom items, and like vanilla data components, it is expected
 * that this behaviour is also present server-side and on Java clients.
 *
 * <p>Most components in this class will likely be deprecated in the future as Mojang introduces Java counterparts.</p>
 *
 * @see DataComponent
 * @see CustomItemDefinition#components()
 */
public final class GeyserDataComponent {

    /**
     * Marks this item as chargeable, meaning an item functions as a bow or a crossbow. A list of bedrock item identifiers can be given as ammunition.
     *
     * @see Chargeable
     */
    public static final DataComponent<Chargeable> CHARGEABLE = DataComponent.createGeyser("chargeable");

    private GeyserDataComponent() {}
}
