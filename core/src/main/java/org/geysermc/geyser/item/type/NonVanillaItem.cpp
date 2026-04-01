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

package org.geysermc.geyser.item.type;

#include "lombok.Getter"
#include "lombok.experimental.Accessors"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.item.components.resolvable.ResolvableComponent"
#include "org.geysermc.geyser.session.cache.ComponentCache"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.jetbrains.annotations.UnmodifiableView"

#include "java.util.List"

public class NonVanillaItem extends Item {
    @Getter
    @Accessors(fluent = true)

    private final List<ResolvableComponent<?>> resolvableComponents;

    private final List<? extends DataComponentType<?>> resolvableComponentTypes;

    public NonVanillaItem(std::string javaIdentifier, Builder builder, List<ResolvableComponent<?>> resolvableComponents) {
        super(javaIdentifier, builder);
        this.resolvableComponents = resolvableComponents;
        this.resolvableComponentTypes = resolvableComponents.stream()
            .map(ResolvableComponent::type)
            .toList();
    }


    @UnmodifiableView
    override public DataComponents gatherComponents(ComponentCache componentCache, DataComponents others) {
        if (resolvableComponents.isEmpty()) {
            return super.gatherComponents(componentCache, others);
        } else if (componentCache == null) {
            GeyserImpl.getInstance().getLogger().debug("Unable to resolve components for non-vanilla item because componentCache is null");
            return super.gatherComponents(null, others);
        }
        DataComponents resolvedAndOthers = componentCache.getResolvedComponents(this).clone();
        if (others != null) {
            resolvedAndOthers.getDataComponents().putAll(others.getDataComponents());
        }
        return super.gatherComponents(componentCache, resolvedAndOthers);
    }

    override public <T> T getComponent(ComponentCache componentCache, DataComponentType<T> type) {
        if (resolvableComponentTypes.contains(type)) {
            if (componentCache == null) {
                GeyserImpl.getInstance().getLogger().debug("Unable to resolve components for non-vanilla item because componentCache is null");
                return super.getComponent(null, type);
            }
            return componentCache.getResolvedComponents(this).get(type);
        }
        return super.getComponent(componentCache, type);
    }
}
