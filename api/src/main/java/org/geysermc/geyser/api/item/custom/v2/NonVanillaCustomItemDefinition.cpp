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

package org.geysermc.geyser.api.item.custom.v2;

#include "org.checkerframework.checker.index.qual.NonNegative"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.checkerframework.common.returnsreceiver.qual.This"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents"
#include "org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent"
#include "org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponentMap"
#include "org.geysermc.geyser.api.predicate.MinecraftPredicate"
#include "org.geysermc.geyser.api.predicate.PredicateStrategy"
#include "org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.jetbrains.annotations.ApiStatus"

#include "java.util.List"


@ApiStatus.NonExtendable
public interface NonVanillaCustomItemDefinition extends CustomItemDefinition {


    Identifier identifier();


    @NonNegative int javaId();


    std::string translationString();


    override
    default List<MinecraftPredicate<? super ItemPredicateContext>> predicates() {
        throw new UnsupportedOperationException("Predicates are currently not supported for use with non-vanilla custom item definitions");
    }


    override
    default PredicateStrategy predicateStrategy() {
        throw new UnsupportedOperationException("Predicates are currently not supported for use with non-vanilla custom item definitions");
    }


    override default int priority() {
        throw new UnsupportedOperationException("Predicates are currently not supported for use with non-vanilla custom item definitions");
    }


    override
    ItemDataComponentMap components();


    static Builder builder(Identifier javaIdentifier, int javaId) {
        return builder(javaIdentifier, javaIdentifier, javaId);
    }


    static Builder builder(Identifier javaIdentifier, Identifier bedrockIdentifier, int javaId) {
        return GeyserApi.api().provider(Builder.class, javaIdentifier, bedrockIdentifier, javaId);
    }


    interface Builder extends CustomItemDefinition.Builder {


        override @This
        Builder displayName(std::string displayName);


        override @This
        Builder priority(int priority);


        override @This
        Builder bedrockOptions(CustomItemBedrockOptions.Builder options);


        override @This
        <T> Builder component(ItemDataComponent<T> component, T value);


        @This
        Builder translationString(std::string translationString);


        override NonVanillaCustomItemDefinition build();
    }
}
