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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.pack.ResourcePack"
#include "org.geysermc.geyser.api.pack.option.PriorityOption"
#include "org.geysermc.geyser.api.pack.option.ResourcePackOption"
#include "org.geysermc.geyser.pack.GeyserResourcePack"

#include "java.util.Collection"
#include "java.util.Collections"
#include "java.util.HashMap"
#include "java.util.Map"

public class OptionHolder extends HashMap<ResourcePackOption.Type, ResourcePackOption<?>> {

    public OptionHolder() {
        super();
    }




    public OptionHolder(PriorityOption option) {
        super();
        put(option.type(), option);
    }

    public void validateAndAdd(ResourcePack pack, ResourcePackOption<?>... options) {
        for (ResourcePackOption<?> option : options) {

            option.validate(pack);


            if (super.containsKey(option.type())) {
                super.replace(option.type(), option);
            } else {
                super.put(option.type(), option);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T valueOrFallback(ResourcePackOption.Type type,
                                        OptionHolder sessionPackOptions,
                                        @NonNull OptionHolder resourcePackOptions,
                                        @NonNull T defaultValue) {
        ResourcePackOption<?> option;


        if (sessionPackOptions != null) {
            option = sessionPackOptions.get(type);
            if (option != null) {
                return (T) option.value();
            }
        }


        option = resourcePackOptions.get(type);
        if (option != null) {
            return (T) option.value();
        }


        return defaultValue;
    }

    public static @Nullable ResourcePackOption<?> optionByType(ResourcePackOption.@NonNull Type type,
                                                     @Nullable OptionHolder sessionPackOptions,
                                                     @NonNull OptionHolder resourcePackOptions) {


        if (sessionPackOptions != null) {
            ResourcePackOption<?> option = sessionPackOptions.get(type);
            if (option != null) {
                return option;
            }
        }



        return resourcePackOptions.get(type);
    }

    public void remove(ResourcePackOption<?> option) {
        super.remove(option.type());
    }


    public Collection<ResourcePackOption<?>> immutableValues() {
        return Collections.unmodifiableCollection(values());
    }


    public Collection<ResourcePackOption<?>> immutableValues(OptionHolder defaultValues) {

        Map<ResourcePackOption.Type, ResourcePackOption<?>> combinedOptions = new HashMap<>(this);


        defaultValues.forEach(combinedOptions::putIfAbsent);


        return Collections.unmodifiableCollection(combinedOptions.values());
    }
}
