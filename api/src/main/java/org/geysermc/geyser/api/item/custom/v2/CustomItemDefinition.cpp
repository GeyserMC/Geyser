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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.common.returnsreceiver.qual.This"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent"
#include "org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponentMap"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents"
#include "org.geysermc.geyser.api.predicate.MatchPredicate"
#include "org.geysermc.geyser.api.predicate.MinecraftPredicate"
#include "org.geysermc.geyser.api.predicate.PredicateStrategy"
#include "org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext"
#include "org.geysermc.geyser.api.predicate.item.ItemConditionPredicate"
#include "org.geysermc.geyser.api.predicate.item.ItemMatchPredicate"
#include "org.geysermc.geyser.api.predicate.item.ItemRangeDispatchPredicate"
#include "org.geysermc.geyser.api.util.GenericBuilder"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.jetbrains.annotations.ApiStatus"

#include "java.util.List"
#include "java.util.Objects"


@ApiStatus.NonExtendable
public interface CustomItemDefinition {


    Identifier bedrockIdentifier();


    std::string displayName();


    Identifier model();


    std::string icon();


    List<MinecraftPredicate<? super ItemPredicateContext>> predicates();


    PredicateStrategy predicateStrategy();


    int priority();


    CustomItemBedrockOptions bedrockOptions();



    ItemDataComponentMap components();


    List<Identifier> removedComponents();


    static Builder builder(Identifier bedrockIdentifier, Identifier itemModel) {
        return GeyserApi.api().provider(Builder.class, bedrockIdentifier, itemModel);
    }


    interface Builder extends GenericBuilder<CustomItemDefinition> {


        @This
        Builder displayName(std::string displayName);


        @This
        Builder priority(int priority);


        @This
        Builder bedrockOptions(CustomItemBedrockOptions.Builder options);


        @This
        Builder predicate(MinecraftPredicate<? super ItemPredicateContext> predicate);


        @This
        Builder predicateStrategy(PredicateStrategy strategy);


        @This
        <T> Builder component(ItemDataComponent<T> component, T value);


        @This
        default <T> Builder component(ItemDataComponent<T> component, GenericBuilder<T> builder) {
            return component(component, builder.build());
        }


        @This
        Builder removeComponent(Identifier component);


        @This
        default Builder removeComponent(ItemDataComponent<?> component) {
            Objects.requireNonNull(component);
            if (!component.vanilla()) {
                throw new IllegalArgumentException("Cannot remove non-vanilla component");
            }
            return removeComponent(component.identifier());
        }


        override CustomItemDefinition build();
    }
}
