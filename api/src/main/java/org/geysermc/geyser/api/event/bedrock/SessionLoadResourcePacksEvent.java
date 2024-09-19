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

package org.geysermc.geyser.api.event.bedrock;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Called when Geyser initializes a session for a new Bedrock client and is in the process of sending resource packs.
 */
public abstract class SessionLoadResourcePacksEvent extends ConnectionEvent {
    public SessionLoadResourcePacksEvent(@NonNull GeyserConnection connection) {
        super(connection);
    }

    /**
     * Gets an unmodifiable list of {@link ResourcePack}'s that will be sent to the client.
     *
     * @return an unmodifiable list of resource packs that will be sent to the client.
     */
    public abstract @NonNull List<ResourcePack> resourcePacks();

    /**
     * Registers a {@link ResourcePack} to be sent to the client.
     *
     * @param pack a resource pack that will be sent to the client
     * @return true if the resource pack was added successfully,
     *         or false if already present
     */
    public abstract boolean register(@NonNull ResourcePack pack);

    /**
     * Registers a {@link ResourcePack} to be sent to the client, alongside
     * specific options.
     *
     * @param resourcePack a resource pack that will be sent to the client.
     * @param options {@link ResourcePackOption}'s that specify how clients load the pack
     * @return true if the resource pack was added successfully,
     *         or false if already present
     */
    public abstract boolean register(@NonNull ResourcePack resourcePack, @Nullable ResourcePackOption<?>... options);

    /**
     * Sets {@link ResourcePackOption}'s for a resource pack
     *
     * @param uuid the resource pack uuid to register the options for
     * @param options the options to register for the pack
     * @throws IllegalArgumentException if the pack is not registered.
     */
    public abstract void registerOptions(@NonNull UUID uuid, @NonNull ResourcePackOption<?>... options);

    /**
     * Returns the subpack options set for a specific resource pack uuid.
     * These are not modifiable.
     *
     * @param uuid the resource pack for which the options are set
     * @return a list of {@link ResourcePackOption}
     */
    public abstract Collection<ResourcePackOption<?>> options(@NonNull UUID uuid);

    /**
     * Returns the current option, or null, for a given ResourcePackOption type.
     *
     * @param uuid the resource pack for which the option type is set
     * @param type the {@link ResourcePackOption.Type} of the option to query
     * @throws IllegalArgumentException if the pack is not registered.
     */
    public abstract @Nullable ResourcePackOption<?> option(@NonNull UUID uuid, ResourcePackOption.@NonNull Type type);

    /**
     * Unregisters a resource pack from being sent to the client.
     *
     * @param uuid the UUID of the resource pack
     * @return true whether the resource pack was removed from the list of resource packs.
     */
    public abstract boolean unregister(@NonNull UUID uuid);
}
