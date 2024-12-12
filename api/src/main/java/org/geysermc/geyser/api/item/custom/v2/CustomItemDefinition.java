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
import org.geysermc.geyser.api.item.custom.v2.predicate.CustomItemPredicate;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.List;

/**
 * This is used to define a custom item and its properties.
 */
// TODO note that definitions will be sorted by predicates
public interface CustomItemDefinition {

    /**
     * The Bedrock identifier for this custom item. This can't be in the {@code minecraft} namespace. If the {@code minecraft} namespace is given in the builder, the default
     * namespace of the implementation is used. For Geyser, the default namespace is the {@code geyser_custom} namespace.
     */
    @NonNull Key bedrockIdentifier();

    /**
     * The display name of the item. If none is set, the display name is taken from the item's Bedrock identifier.
     */
    @NonNull String displayName();

    /**
     * The item model this definition is for. If the model is in the {@code minecraft} namespace, then the definition is required to have a predicate.
     *
     * <p>If multiple item definitions for a model are registered, then only one can have no predicate.</p>
     */
    @NonNull Key model();

    /**
     * The icon used for this item.
     *
     * <p>If none is set in the item's Bedrock options, then the item's Bedrock identifier is used,
     * the namespace separator replaced with {@code .} and the path separators ({@code /}) replaced with {@code _}.</p>
     */
    default @NonNull String icon() {
        return bedrockOptions().icon() == null ? bedrockIdentifier().asString().replaceAll(":", ".").replaceAll("/", "_") : bedrockOptions().icon();
    }

    /**
     * The predicates that have to match for this item to be used. These predicates are similar to the Java item model predicates.
     *
     * <p>If all predicates match for multiple definitions, then the first registered item with all matching predicates is used. If no predicates match, then the item definition without any predicates
     * is used, if any.</p>
     */
    @NonNull List<CustomItemPredicate> predicates();

    /**
     * The priority of this definition. For all definitions for a single Java item model, definitions with a higher priority will be matched first. Defaults to 0.
     */
    int priority();

    /**
     * The item's Bedrock options. These describe item properties that can't be described in item components, e.g. item texture size and if the item is allowed in the off-hand.
     */
    @NonNull CustomItemBedrockOptions bedrockOptions();

    /**
     * The item's data components. It is expected that the item <em>always</em> has these components on the server. If the components mismatch, bugs will occur.
     *
     * <p>Currently, the following components are supported:</p>
     *
     * <ul>
     *     <li>{@code minecraft:consumable}</li>
     *     <li>{@code minecraft:equippable}</li>
     *     <li>{@code minecraft:food}</li>
     *     <li>{@code minecraft:max_damage}</li>
     *     <li>{@code minecraft:max_stack_size}</li>
     *     <li>{@code minecraft:use_cooldown}</li>
     * </ul>
     *
     * <p>Note: some components, for example {@code minecraft:rarity}, {@code minecraft:enchantment_glint_override}, and {@code minecraft:attribute_modifiers} are translated automatically,
     * and do not have to be specified here.</p>
     */
    @NonNull DataComponents components();

    static Builder builder(Key identifier, Key itemModel) {
        return GeyserApi.api().provider(Builder.class, identifier, itemModel);
    }

    interface Builder {

        Builder displayName(String displayName);

        Builder predicate(@NonNull CustomItemPredicate predicate);

        Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options);

        Builder priority(int priority);

        // TODO do we want another format for this?
        Builder components(@NonNull DataComponents components);

        CustomItemDefinition build();
    }
}