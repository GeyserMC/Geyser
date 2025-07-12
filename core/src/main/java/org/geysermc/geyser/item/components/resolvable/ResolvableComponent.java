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

package org.geysermc.geyser.item.components.resolvable;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

/**
 * A resolvable component is a component that was specified in a {@link org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition}, and was supposed to be mapped to its MCPL equivalent before registering
 * the item, but was unable to because it needed registry access. As such, an instance of this interface was created, added to the list of the {@link org.geysermc.geyser.item.type.NonVanillaItem}'s resolvable components,
 * and will be resolved by {@link org.geysermc.geyser.session.cache.ComponentCache} whenever the session finishes the configuration phase.
 *
 * <p>Resolvable components aren't needed for vanilla-item overrides, because there Geyser receives the component patch in MCPL format from the server as well.</p>
 */
public interface ResolvableComponent<T> {

    DataComponentType<T> type();

    T resolve(GeyserSession session);

    default void resolve(GeyserSession session, DataComponents map) {
        map.put(type(), resolve(session));
    }
}
