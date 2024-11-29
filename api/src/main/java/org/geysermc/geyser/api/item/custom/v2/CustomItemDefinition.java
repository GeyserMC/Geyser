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

package org.geysermc.geyser.api.item.custom.v2;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

/**
 * This is used to store data for a custom item.
 *
 * V2. TODO.
 */
public interface CustomItemDefinition {

    /**
     * Gets the item model this definition is for. This model can't be in the Minecraft namespace.
     */
    @NonNull Key model(); // TODO name??

    default String name() {
        return model().namespace() + "_" + model().value();
    } // TODO, also display name ? also, rename to identifier

    default String icon() {
        return bedrockOptions().icon() == null ? name() : bedrockOptions().icon();
    } // TODO

    // TODO predicate

    // TODO bedrock options

    @NonNull CustomItemBedrockOptions bedrockOptions();

    // TODO components

    @NonNull DataComponents components();

    static Builder builder(Key itemModel) {
        return GeyserApi.api().provider(Builder.class, itemModel);
    }

    interface Builder {

        Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options);

        // TODO do we want another format for this?
        Builder components(@NonNull DataComponents components);

        CustomItemDefinition build();
    }
}
