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
import org.geysermc.geyser.api.pack.exception.ResourcePackException;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Called when Geyser initializes a session for a new Bedrock client and is in the process of sending {@link ResourcePack}'s.
 * @since 2.1.1
 */
public abstract class SessionLoadResourcePacksEvent extends ConnectionEvent {
    public SessionLoadResourcePacksEvent(@NonNull GeyserConnection connection) {
        super(connection);
    }

    /**
     * Gets the {@link ResourcePack}'s that will be sent to this {@link GeyserConnection}.
     * To remove packs, use {@link #unregister(UUID)}, as the list returned
     * by this method is unmodifiable.
     *
     * @return an unmodifiable list of {@link ResourcePack}'s
     * @since 2.1.1
     */
    public abstract @NonNull List<ResourcePack> resourcePacks();

    /**
     * @deprecated Use {{@link #register(ResourcePack, ResourcePackOption[])}} instead
     */
    @Deprecated
    public abstract boolean register(@NonNull ResourcePack pack);

    /**
     * Registers a {@link ResourcePack} to be sent to the client, optionally alongside
     * specific {@link ResourcePackOption}'s specifying how it will be applied by the client.
     *
     * @param pack the {@link ResourcePack} that will be sent to the client
     * @param options {@link ResourcePackOption}'s that specify how the client loads the pack
     * @throws ResourcePackException if an issue occurred during pack registration
     * @since 2.6.2
     */
    public abstract void register(@NonNull ResourcePack pack, @Nullable ResourcePackOption<?>... options);

    /**
     * Sets {@link ResourcePackOption}'s for a {@link ResourcePack}.
     * This method can also be used to override options for resource packs already registered in the
     * {@link org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent}.
     *
     * @param uuid the uuid of the resource pack to register the options for
     * @param options the {@link ResourcePackOption}'s to register for the resource pack
     * @throws ResourcePackException if an issue occurred during {@link ResourcePackOption} registration
     * @since 2.6.2
     */
    public abstract void registerOptions(@NonNull UUID uuid, @NonNull ResourcePackOption<?>... options);

    /**
     * Returns a collection of {@link ResourcePackOption}'s for a registered {@link ResourcePack}.
     * The collection returned here is not modifiable.
     *
     * @param uuid the {@link ResourcePack} for which the options are set
     * @return a collection of {@link ResourcePackOption}'s
     * @throws ResourcePackException if the pack was not registered
     * @since 2.6.2
     */
    public abstract Collection<ResourcePackOption<?>> options(@NonNull UUID uuid);

    /**
     * Returns the current {@link ResourcePackOption}, or null, for a given {@link ResourcePackOption.Type}.
     *
     * @param uuid the {@link ResourcePack} for which to query this option type
     * @param type the {@link ResourcePackOption.Type} of the option to query
     * @throws ResourcePackException if the queried option is invalid or not present on the resource pack
     * @since 2.6.2
     */
    public abstract @Nullable ResourcePackOption<?> option(@NonNull UUID uuid, ResourcePackOption.@NonNull Type type);

    /**
     * Unregisters a {@link ResourcePack} from the list of packs sent to this {@link GeyserConnection}.
     *
     * @param uuid the UUID of the {@link ResourcePack} to be removed
     * @since 2.1.1
     */
    public abstract boolean unregister(@NonNull UUID uuid);

    /**
     * Whether to forcefully disable vibrant visuals for joining clients.
     * While vibrant visuals are nice to look at, they can cause issues with
     * some resource packs.
     *
     * @param enabled Whether vibrant visuals are allowed. This is true by default.
     * @since 2.7.2
     */
    public abstract void allowVibrantVisuals(boolean enabled);
}
