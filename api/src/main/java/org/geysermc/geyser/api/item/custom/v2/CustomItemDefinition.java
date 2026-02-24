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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponentMap;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents;
import org.geysermc.geyser.api.predicate.MatchPredicate;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.item.ItemConditionPredicate;
import org.geysermc.geyser.api.predicate.item.ItemMatchPredicate;
import org.geysermc.geyser.api.predicate.item.ItemRangeDispatchPredicate;
import org.geysermc.geyser.api.util.GenericBuilder;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Objects;

/**
 * This is used to define a custom item and its properties for a specific Java item and item model definition combination.
 *
 * <p>A custom Bedrock item definition will be used when translating any item stack that matches the vanilla Java item this definition is registered for,
 * and the item model this item definition has specified.
 * Additionally, predicates can be added that allow fine-grained control as to when to use this custom item. These predicates are similar
 * to the predicates available in Java item model definitions.</p>
 *
 * <p>In Geyser, all registered custom item definitions for a Java item model will be checked in a specific order:</p>
 *
 * <ol>
 *     <li>First by checking their priority values, higher priority values going first.</li>
 *     <li>Then by checking if they both have a similar {@link ItemRangeDispatchPredicate} predicate, the one with the highest (or lowest, when both are negated) threshold going first.</li>
 *     <li>Lastly by the amount of predicates, from most to least.</li>
 * </ol>
 *
 * <p>Please note! While this system in most cases ensures predicates will be checked in the correct order,
 * the range dispatch predicate sorting only works when 2 definitions only have 1 range dispatch predicate that is similar enough. With more complicated predicate checks,
 * it is recommended to make use of priority values, to ensure the intended order.</p>
 *
 * @since 2.9.3
 */
@ApiStatus.NonExtendable
public interface CustomItemDefinition {

    /**
     * The Bedrock identifier for this custom item. It cannot be in the {@code minecraft} namespace.
     *
     * @return the Bedrock item identifier
     * @since 2.9.3
     */
    @NonNull Identifier bedrockIdentifier();

    /**
     * The display name of the item. If none is set, the display name is taken from the item's Bedrock identifier.
     *
     * @return the display name shown to Bedrock clients
     * @since 2.9.3
     */
    @NonNull String displayName();

    /**
     * The item model this definition is for. If the model is in the {@code minecraft} namespace, then the definition must have at least one predicate.
     *
     * <p>If multiple item definitions for a model are registered, then only one can have no predicate.</p>
     *
     * @return the identifier of the Java item model used to match this definition
     * @since 2.9.3
     */
    @NonNull Identifier model();

    /**
     * The icon used for this item.
     *
     * <p>If none is set in the item's Bedrock options, then the item's Bedrock identifier is used,
     * the namespace separator ({@code :}) replaced with {@code .} and the path separators ({@code /}) replaced with {@code _}. For example:</p>
     *
     * <p>{@code my_datapack:my_custom_item} => {@code my_datapack.my_custom_item}</p>
     * <p>{@code my_datapack:cool_items/cool_item_1} => {@code my_datapack.cool_items_cool_item_1}</p>
     *
     * @return the icon shown to Bedrock players
     * @since 2.9.3
     */
    @NonNull String icon();

    /**
     * The predicates that have to match for this item definition to be used. These predicates can access properties similar to the Java item model predicates.
     *
     * <p>When adding predicates, avoid chaining many predicates that use an OR expression - instead, set the {@link PredicateStrategy} of the definition to
     * {@link PredicateStrategy#OR}.</p>
     *
     * <p>It is recommended to use built-in predicates created from classes such as {@link MatchPredicate}, {@link ItemMatchPredicate},
     * {@link ItemRangeDispatchPredicate}, and {@link ItemConditionPredicate} when possible. These predicates
     * have built in conflict detection, value caching, and, in the case of range dispatch predicates, proper predicate sorting. This makes bugs easier to discover, and is generally more performant.</p>
     *
     * @since 2.9.3
     */
    @NonNull List<MinecraftPredicate<? super ItemPredicateContext>> predicates();

    /**
     * The predicate strategy used when evaluating predicates. Determines if one of, or all of the predicates have to pass for this item definition to be used. Defaults to {@link PredicateStrategy#AND}.
     *
     * @since 2.9.3
     */
    @NonNull PredicateStrategy predicateStrategy();

    /**
     * @return the priority of this definition. For all definitions for a single Java item model, definitions with a higher priority will be matched first. Defaults to 0.
     * @since 2.9.3
     */
    int priority();

    /**
     * @return the item's Bedrock options. These describe item properties that can't be described in item components, e.g. item texture size and if the item is allowed in the off-hand.
     * @since 2.9.3
     */
    @NonNull CustomItemBedrockOptions bedrockOptions();

    /**
     * The item's data components. It is expected that the item <em>always</em> has these components on the server. If the components mismatch, bugs will occur.
     *
     * <p>Currently, the following components are (somewhat) supported:</p>
     *
     * <ul>
     *     <li>{@code minecraft:consumable} ({@link JavaItemDataComponents#CONSUMABLE})</li>
     *     <li>{@code minecraft:equippable} ({@link JavaItemDataComponents#EQUIPPABLE})</li>
     *     <li>{@code minecraft:food} ({@link JavaItemDataComponents#FOOD})</li>
     *     <li>{@code minecraft:max_damage} ({@link JavaItemDataComponents#MAX_DAMAGE})</li>
     *     <li>{@code minecraft:max_stack_size} ({@link JavaItemDataComponents#MAX_STACK_SIZE})</li>
     *     <li>{@code minecraft:use_cooldown} ({@link JavaItemDataComponents#USE_COOLDOWN})</li>
     *     <li>{@code minecraft:enchantable} ({@link JavaItemDataComponents#ENCHANTABLE})</li>
     *     <li>{@code minecraft:tool} ({@link JavaItemDataComponents#TOOL})</li>
     *     <li>{@code minecraft:repairable} ({@link JavaItemDataComponents#REPAIRABLE})</li>
     *     <li>{@code minecraft:enchantment_glint_override} ({@link JavaItemDataComponents#ENCHANTMENT_GLINT_OVERRIDE})</li>
     *     <li>{@code minecraft:attack_range} ({@link JavaItemDataComponents#ATTACK_RANGE})</li>
     *     <li>{@code minecraft:piercing_weapon} ({@link JavaItemDataComponents#PIERCING_WEAPON})</li>
     *     <li>{@code minecraft:use_effects} ({@link JavaItemDataComponents#USE_EFFECTS})</li>
     *     <li>{@code minecraft:kinetic_weapon} ({@link JavaItemDataComponents#KINETIC_WEAPON}</li>
     * </ul>
     *
     * <p>Note: some components, for example {@code minecraft:rarity} and {@code minecraft:attribute_modifiers}, are translated automatically, and do not have to be specified here.
     * Components that are added here cannot be removed in {@link CustomItemDefinition#removedComponents()}.</p>
     *
     * @see JavaItemDataComponents
     * @see CustomItemDefinition#removedComponents()
     * @return the item's data component patch
     * @since 2.9.3
     */
    @NonNull
    ItemDataComponentMap components();

    /**
     * A list of removed default item data components. These are components that are present on the vanilla base item, but not on the custom item. Like with custom added
     * components, it is expected that this <em>always</em> matches the removed components on the server. Removed components cannot be present in the added components in {@link CustomItemDefinition#components()}.
     *
     * @see CustomItemDefinition#components()
     * @return a list of removed default item data components
     * @since 2.9.3
     */
    @NonNull List<Identifier> removedComponents();

    /**
     * Creates a builder for the custom item definition.
     *
     * @param bedrockIdentifier the Bedrock item identifier
     * @param itemModel the Java item model identifier
     * @see CustomItemDefinition#bedrockIdentifier()
     * @see CustomItemDefinition#model()
     * @return a new builder
     * @since 2.9.3
     */
    static Builder builder(@NonNull Identifier bedrockIdentifier, @NonNull Identifier itemModel) {
        return GeyserApi.api().provider(Builder.class, bedrockIdentifier, itemModel);
    }

    /**
     * The builder for the custom item definition.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<CustomItemDefinition> {

        /**
         * Sets the display name, as shown to the Bedrock client.
         * When not set, the display name will be derived from the Bedrock item identifier.
         *
         * @param displayName the display name to show for Bedrock clients.
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder displayName(@NonNull String displayName);

        /**
         * Sets the priority of this definition, used for definition matching.
         * @see CustomItemDefinition#priority()
         *
         * @param priority the priority
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder priority(int priority);

        /**
         * Sets the Bedrock item options for this definition.
         * Those determine the icon seen on Bedrock edition, whether the item
         * can be placed in the offhand slot, and other options.
         *
         * @see CustomItemBedrockOptions
         * @param options the bedrock item options
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options);

        /**
         * Adds a predicate that must match for Geyser to use this item definition.
         * See {@link CustomItemDefinition#predicates()} for details.
         *
         * @param predicate a predicate that must match for this item to be used
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder predicate(@NonNull MinecraftPredicate<? super ItemPredicateContext> predicate);

        /**
         * Sets the predicate strategy that should be used for item definition matching.
         *
         * @param strategy the predicate strategy to use
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder predicateStrategy(@NonNull PredicateStrategy strategy);

        /**
         * Sets data components that determine the item behavior. These are assumed to also be
         * present server-side on the Java server. See {@link CustomItemDefinition#components()}
         * for more information.
         *
         * <p>Added data components cannot be removed using {@link CustomItemDefinition.Builder#removeComponent(Identifier)},
         * and this method will throw when a component is added that was removed using the aforementioned method.</p>
         *
         * @param component the type of the component, found in {@link JavaItemDataComponents}
         * @param value the value of the component
         * @param <T> the value held by the component
         * @throws IllegalArgumentException when the added component was removed using {@link CustomItemDefinition.Builder#removeComponent(Identifier)}
         * @return this builder
         * @since 2.9.3
         */
        @This
        <T> Builder component(@NonNull ItemDataComponent<T> component, @NonNull T value);

        /**
         * Convenience method for {@link CustomItemDefinition.Builder#component(ItemDataComponent, Object)}
         *
         * @param component the type of the component - found in {@link JavaItemDataComponents}
         * @param builder the builder of the component
         * @param <T> the value held by the component
         * @throws IllegalArgumentException when the added component was removed using {@link CustomItemDefinition.Builder#removeComponent(Identifier)}
         * @see CustomItemDefinition.Builder#component(ItemDataComponent, Object)
         * @return this builder
         * @since 2.9.3
         */
        @This
        default <T> Builder component(@NonNull ItemDataComponent<T> component, @NonNull GenericBuilder<T> builder) {
            return component(component, builder.build());
        }

        /**
         * Indicates a removed item component that will not be present on the custom item despite
         * existing on the vanilla item. This must match server-side behavior, otherwise, issues
         * will occur. See {@link CustomItemDefinition#removedComponents()} for more information.
         *
         * <p>Removed data components cannot be added again using {@link CustomItemDefinition.Builder#component(ItemDataComponent, Object)},
         * and this method will throw when a component is removed that was added using the aforementioned method.</p>
         *
         * @param component the identifier of the vanilla base component to remove
         * @throws IllegalArgumentException when the removed component was added using {@link CustomItemDefinition.Builder#component(ItemDataComponent, Object)}
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder removeComponent(@NonNull Identifier component);

        /**
         * Convenience method for {@link CustomItemDefinition.Builder#removeComponent(Identifier)}.
         *
         * @param component the component type to remove
         * @throws IllegalArgumentException when the removed component was added using {@link CustomItemDefinition.Builder#component(ItemDataComponent, Object)}
         * @return this builder
         * @since 2.9.3
         */
        @This
        default Builder removeComponent(@NonNull ItemDataComponent<?> component) {
            Objects.requireNonNull(component);
            if (!component.vanilla()) {
                throw new IllegalArgumentException("Cannot remove non-vanilla component");
            }
            return removeComponent(component.identifier());
        }

        /**
         * Creates the custom item definition.
         *
         * @return the new custom item definition
         * @since 2.9.3
         */
        @Override
        CustomItemDefinition build();
    }
}
