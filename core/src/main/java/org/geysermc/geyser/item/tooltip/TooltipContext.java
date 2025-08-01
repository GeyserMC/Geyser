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

package org.geysermc.geyser.item.tooltip;

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.item.TooltipOptions;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.Optional;
import java.util.function.Consumer;

public record TooltipContext(Optional<GeyserSession> session, boolean advanced, boolean creative, Item item, DataComponents components,
                             TooltipOptions options) {

    public <T> void getRegistryEntry(JavaRegistryKey<T, ?> registry, Key key, Consumer<T> consumer) {
        session.flatMap(session -> Optional.ofNullable(registry.value(session, key))).ifPresent(consumer);
    }

    public <T, MCPL> void getRegistryEntry(JavaRegistryKey<T, MCPL> registry, Holder<MCPL> holder, Consumer<T> consumer) {
        session.flatMap(session -> Optional.ofNullable(registry.value(session, holder))).ifPresent(consumer);
    }

    public void withSession(Consumer<GeyserSession> consumer) {
        session.ifPresent(consumer);
    }

    public TooltipContext withItemComponents(Item item, DataComponents components) {
        return new TooltipContext(session, advanced, creative, item, components, options);
    }

    public TooltipContext withFlags(boolean advanced, boolean creative) {
        return new TooltipContext(session, advanced, creative, item, components, options);
    }

    public static TooltipContext create(GeyserSession session, Item item, DataComponents components) {
        return new TooltipContext(Optional.of(session), session.isAdvancedTooltips(), false, item, components, TooltipOptions.fromComponents(components));
    }

    public static TooltipContext createForCreativeMenu(Item item) {
        DataComponents components = item.gatherComponents(null);
        return new TooltipContext(Optional.empty(), false, true, item, components, TooltipOptions.fromComponents(components));
    }
}
