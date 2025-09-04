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

import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.session.cache.ComponentCache;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public class NonVanillaItem extends Item {
    @Getter
    @Accessors(fluent = true)
    @NonNull
    private final List<ResolvableComponent<?>> resolvableComponents;
    @NonNull
    private final List<? extends DataComponentType<?>> resolvableComponentTypes;

    public NonVanillaItem(String javaIdentifier, Builder builder, @NonNull List<ResolvableComponent<?>> resolvableComponents) {
        super(javaIdentifier, builder);
        this.resolvableComponents = resolvableComponents;
        this.resolvableComponentTypes = resolvableComponents.stream()
            .map(ResolvableComponent::type)
            .toList();
    }

    @NonNull
    @UnmodifiableView
    @Override
    public DataComponents gatherComponents(@Nullable ComponentCache componentCache, @Nullable DataComponents others) {
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

    @Override
    public <T> @Nullable T getComponent(@Nullable ComponentCache componentCache, @NonNull DataComponentType<T> type) {
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
