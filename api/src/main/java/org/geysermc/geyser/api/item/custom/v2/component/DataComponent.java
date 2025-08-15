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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.java.ItemDataComponents;
import org.geysermc.geyser.api.util.GeyserProvided;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Predicate;

/**
 * Data components are used to indicate item behaviour of custom items.
 * It is expected that any components set on a {@link CustomItemDefinition} are always present on the item server-side.
 *
 * @see ItemDataComponents
 * @see GeyserDataComponent
 * @see CustomItemDefinition#components()
 */
@ApiStatus.NonExtendable
public interface DataComponent<T> extends GeyserProvided {

    /**
     * The identifier of the data component.
     *
     * @return the identifier
     */
    @NonNull
    Identifier identifier();

    /**
     * The predicate used to validate the component.
     *
     * @return the validator
     */
    @NonNull
    Predicate<T> validator();

    /**
     * Whether the component exists in vanilla Minecraft.
     *
     * @return whether this component is vanilla
     */
    boolean vanilla();
}
