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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.item.type.NonVanillaItem;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.List;

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
                System.out.println("item " + nonVanilla.javaKey() + " is a candidate to resolve components for");
                List<ResolvableComponent<?>> toResolve = nonVanilla.resolvableComponents();
                System.out.println("components to resolve: " + toResolve);
                if (!toResolve.isEmpty()) {
                    System.out.println("going to resolve");
                    DataComponents resolved = new DataComponents(new Object2ObjectOpenHashMap<>());
                    for (ResolvableComponent<?> component : toResolve) {
                        System.out.println("resolving " + component);
                        component.resolve(session, resolved);
                    }
                    resolvedComponents.put(nonVanilla, resolved);
                    System.out.println("put in map for " + nonVanilla.javaKey() + " " + resolved);
                } else {
                    System.out.println("no components to resolve");
                }
            }
        }
    }
}
