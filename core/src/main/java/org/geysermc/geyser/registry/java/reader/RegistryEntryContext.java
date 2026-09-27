/*
 * Copyright (c) 2024-2026 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;

import java.util.Optional;

/**
 * Expands {@link JavaRegistryReader.Context} with a {@link Key}. Used when parsing registry data from a {@link ClientboundRegistryDataPacket}.
 *
 * @param registries the {@link JavaRegistryProvider}
 * @param entry the {@link RegistryEntry}
 * @param session the {@link GeyserSession}
 */
public record RegistryEntryContext(JavaRegistryProvider registries, RegistryEntry entry, Optional<GeyserSession> session) implements JavaRegistryReader.Context {

    /**
     * @return the {@link Key} of this registry entry
     */
    public Key id() {
        return entry.getId();
    }

    // Not annotated as nullable because data should never be null here
    @Override
    public Object data() {
        return entry.getData();
    }
}
