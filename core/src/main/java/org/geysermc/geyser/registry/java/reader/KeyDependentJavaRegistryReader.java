/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.java.reader;

import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

/**
 * This interface weakens the definitions defined in {@link JavaRegistryReader}, by taking a {@link RegistryEntryContext} as {@link JavaRegistryReader.Context},
 * which provides {@link RegistryEntryContext#id()} to access the {@link Key} of the registry entry being parsed. This is useful when
 * trying to map dynamic Java content to static bedrock content.
 *
 * <p>Generally try to avoid the need on this interface though: because the reader depends on a {@link Key} for a registry entry, it cannot be used when parsing inline
 * registry entries, such as from {@link Holder}s or {@link HolderSet}s.</p>
 *
 * @param <T> the type this reader parses into
 * @see RegistryEntryContext
 * @see JavaRegistryReaders
 * @see JavaRegistryReader
 */
@FunctionalInterface
public interface KeyDependentJavaRegistryReader<T> {

    T read(RegistryEntryContext context);
}
