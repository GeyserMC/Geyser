/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.event.lifecycle;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.util.TriState;

/**
 * Fired by anything that wishes to gather permission nodes and defaults.
 * <p>
 * This event is not guaranteed to be fired, as certain Geyser platforms do not have a native permission system.
 * It can be expected to fire on Geyser-Spigot, Geyser-NeoForge, Geyser-Standalone, and Geyser-ViaProxy
 * It may be fired by a 3rd party regardless of the platform.
 */
public interface GeyserRegisterPermissionsEvent extends Event {

    /**
     * Registers a permission node and its default value with the firer.<p>
     * {@link TriState#TRUE} corresponds to all players having the permission by default.<br>
     * {@link TriState#NOT_SET} corresponds to only server operators having the permission by default (if such a concept exists on the platform).<br>
     * {@link TriState#FALSE} corresponds to no players having the permission by default.<br>
     *
     * @param permission the permission node to register
     * @param defaultValue the default value of the node
     */
    void register(@NonNull String permission, @NonNull TriState defaultValue);
}
