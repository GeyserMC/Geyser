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

package org.geysermc.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.session.Session;

import java.util.List;
import java.util.UUID;

/**
 * The base API class.
 *
 * @param <S> the session type
 */
public interface Api<S extends Session> {

    /**
     * Gets the session from the given
     * UUID, if applicable.
     *
     * @param uuid the UUID of the session
     * @return the session from the given UUID, if applicable
     */
    @Nullable
    S sessionByUuid(@NonNull UUID uuid);

    /**
     * Gets the session from the given
     * XUID, if applicable.
     *
     * @param xuid the XUID of the session
     * @return the session from the given UUID, if applicable
     */
    @Nullable
    S sessionByXuid(@NonNull String xuid);

    /**
     * Gets the session from the given
     * name, if applicable.
     *
     * @param name the uuid of the session
     * @return the session from the given name, if applicable
     */
    @Nullable
    S sessionByName(@NonNull String name);

    /**
     * Gets all the online sessions.
     *
     * @return all the online sessions
     */
    @NonNull
    List<S> onlineSessions();
}
