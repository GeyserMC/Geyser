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

package org.geysermc.geyser.api.network;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base class for all Geyser networking API failures.
 * <p>
 * Each exception carries an optional {@link #source() source} identifying who
 * is responsible for the failure (typically the namespace of the extension or
 * plugin that owns the channel involved). The {@linkplain #getMessage() detail
 * message} is written to be readable by server administrators who are not
 * developers, so they know which third party to contact.
 *
 * @since 2.9.2
 */
public class NetworkApiException extends RuntimeException {

    private final @Nullable String source;

    public NetworkApiException(@Nullable String source, String message) {
        super(formatMessage(source, message));
        this.source = source;
    }

    public NetworkApiException(@Nullable String source, String message, Throwable cause) {
        super(formatMessage(source, message), cause);
        this.source = source;
    }

    /**
     * Returns the namespace of the party responsible for this failure.
     * <p>
     * For extension-owned channels this is the extension id. For inbound
     * custom payloads originating from a Java server plugin or mod this is
     * the namespace of the payload. {@code null} indicates the responsible
     * party could not be identified.
     *
     * @return the responsible party, or {@code null} if not known
     */
    public @Nullable String source() {
        return this.source;
    }

    private static String formatMessage(@Nullable String source, String message) {
        if (source == null || source.isBlank()) {
            return "[Geyser Network API] " + message;
        }
        return "[Geyser Network API] " + message + " (responsible party: '" + source + "')";
    }
}
