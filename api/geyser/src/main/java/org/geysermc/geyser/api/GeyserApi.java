/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.List;
import java.util.UUID;

/**
 * Represents the API used in Geyser.
 */
public interface GeyserApi extends GeyserApiBase {
    /**
     * Shuts down the current Geyser instance.
     */
    void shutdown();

    /**
     * Reloads the current Geyser instance.
     */
    void reload();

    /**
     * Gets if this Geyser instance is running in an IDE. This only needs to be used in cases where files
     * expected to be in a jarfile are not present.
     *
     * @return true if the version number is not 'DEV'.
     */
    boolean productionEnvironment();

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable GeyserConnection connectionByUuid(@NonNull UUID uuid);

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable GeyserConnection connectionByXuid(@NonNull String xuid);

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable GeyserConnection connectionByName(@NonNull String name);

    /**
     * {@inheritDoc}
     */
    @NonNull
    List<? extends GeyserConnection> onlineConnections();
}
