/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.pack.option;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;
import org.geysermc.geyser.pack.GeyserResourcePack;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OptionHolder extends HashMap<ResourcePackOption.Type, ResourcePackOption<?>> {

    public void add(ResourcePackOption<?> option) {
        if (super.containsKey(option.type())) {
            super.replace(option.type(), option);
        } else {
            super.put(option.type(), option);
        }
    }

    public void add(ResourcePackOption<?>... options) {
        for (ResourcePackOption<?> option : options) {
            add(option);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getWithFallbacks(ResourcePackOption.@NonNull Type type,
                                         @Nullable OptionHolder holder,
                                         @NonNull OptionHolder defaultHolder,
                                         @NonNull T defaultValue) {
        ResourcePackOption<?> option;

        // First: the optionHolder's option, if it exists
        if (holder != null) {
            option = holder.get(type);
            if (option != null) {
                return ((ResourcePackOption<T>) option).value();
            }
        }

        // Second: check the default optionHolder for the option, if it exists
        option = defaultHolder.get(type);
        if (option != null) {
            return ((ResourcePackOption<T>) option).value();
        }

        // Finally: Fallback to default
        return defaultValue;
    }

    public void remove(ResourcePackOption<?> option) {
        super.remove(option.type());
    }

    public OptionHolder() {
        super();
        add(PriorityOption.NORMAL);
    }

    public void validateOptions(ResourcePack pack) {
        values().forEach(option -> option.validate(pack));
    }

    /**
     * @return the options of this option optionHolder in an immutable collection
     */
    public Collection<ResourcePackOption<?>> immutableValues() {
        return Collections.unmodifiableCollection(values());
    }

    /**
     * @return the options of this option optionHolder, with fallbacks to options of a {@link GeyserResourcePack}
     * if they're not already overridden here
     */
    public Collection<ResourcePackOption<?>> immutableValues(OptionHolder defaultValues) {
        if (defaultValues.isEmpty()) {
            return immutableValues();
        }

        // Create a map to hold the combined values
        Map<ResourcePackOption.Type, ResourcePackOption<?>> combinedOptions = new HashMap<>(this);

        // Add options from the pack if not already overridden by this OptionHolder
        defaultValues.forEach(combinedOptions::putIfAbsent);

        // Return an immutable collection of the combined options
        return Collections.unmodifiableCollection(combinedOptions.values());
    }
}
