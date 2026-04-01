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

package org.geysermc.geyser.session.cache;

#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.Reference2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap"
#include "org.geysermc.geyser.item.components.resolvable.ResolvableComponent"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.item.type.NonVanillaItem"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"

#include "java.util.List"

public class ComponentCache {
    private final GeyserSession session;
    private final Reference2ObjectMap<NonVanillaItem, DataComponents> resolvedComponents = new Reference2ObjectOpenHashMap<>();

    public ComponentCache(GeyserSession session) {
        this.session = session;
    }

    public DataComponents getResolvedComponents(NonVanillaItem item) {
        return resolvedComponents.get(item);
    }

    public void resolveComponents() {
        resolvedComponents.clear();
        for (Item item : Registries.JAVA_ITEMS.get()) {
            if (item instanceof NonVanillaItem nonVanilla) {
                List<ResolvableComponent<?>> toResolve = nonVanilla.resolvableComponents();
                if (!toResolve.isEmpty()) {
                    DataComponents resolved = new DataComponents(new Object2ObjectOpenHashMap<>());
                    for (ResolvableComponent<?> component : toResolve) {
                        component.resolve(session, resolved);
                    }
                    resolvedComponents.put(nonVanilla, resolved);
                }
            }
        }
    }
}
