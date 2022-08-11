/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;
import org.geysermc.api.connection.Connection;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;

import java.util.List;
import java.util.UUID;

/**
 * The base API class.
 */
public interface GeyserApiBase {
    /**
     * Gets the connection from the given UUID, if applicable. The player must be logged in to the Java server
     * for this to return a non-null value.
     *
     * @param uuid the UUID of the connection
     * @return the connection from the given UUID, if applicable
     */
    @Nullable
    Connection connectionByUuid(@NonNull UUID uuid);

    /**
     * Gets the connection from the given XUID, if applicable. This method only works for online connections.
     *
     * @param xuid the XUID of the session
     * @return the connection from the given UUID, if applicable
     */
    @Nullable
    Connection connectionByXuid(@NonNull String xuid);

    /**
     * Method to determine if the given <b>online</b> player is a Bedrock player.
     *
     * @param uuid the uuid of the online player
     * @return true if the given online player is a Bedrock player
     */
    boolean isBedrockPlayer(@NonNull UUID uuid);

    /**
     * Sends a form to the given connection and opens it.
     *
     * @param uuid the uuid of the connection to open it on
     * @param form the form to send
     * @return whether the form was successfully sent
     */
    boolean sendForm(@NonNull UUID uuid, @NonNull Form form);

    /**
     * Sends a form to the given connection and opens it.
     *
     * @param uuid        the uuid of the connection to open it on
     * @param formBuilder the formBuilder to send
     * @return whether the form was successfully sent
     */
    boolean sendForm(@NonNull UUID uuid, @NonNull FormBuilder<?, ?, ?> formBuilder);

    /**
     * Transfer the given connection to a server. A Bedrock player can successfully transfer to the same server they are
     * currently playing on.
     *
     * @param uuid    the uuid of the connection
     * @param address the address of the server
     * @param port    the port of the server
     * @return true if the transfer was a success
     */
    boolean transfer(@NonNull UUID uuid, @NonNull String address, @IntRange(from = 0, to = 65535) int port);


    /**
     * Returns all the online connections.
     */
    @NonNull
    List<? extends Connection> onlineConnections();

    /**
     * Returns the amount of online connections.
     */
    int onlineConnectionsCount();

    /**
     * Returns the prefix used by Floodgate. Will be null when the auth-type isn't Floodgate.
     */
    @MonotonicNonNull
    String usernamePrefix();

    /**
     * Returns the major API version. Bumped whenever a significant breaking change or feature addition is added.
     */
    default int majorApiVersion() {
        return 1;
    }

    /**
     * Returns the minor API version. May be bumped for new API additions.
     */
    default int minorApiVersion() {
        return 0;
    }
}
