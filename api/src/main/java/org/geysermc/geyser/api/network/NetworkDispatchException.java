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
 * Thrown when a message handler, encoder, or decoder fails while a network
 * message is being dispatched through Geyser.
 * <p>
 * The original failure is always preserved as the {@linkplain #getCause()
 * cause} so the underlying stack trace remains intact when this exception
 * surfaces through Netty's pipeline. Use {@link #source()} to identify which
 * extension or plugin is at fault.
 *
 * @since 2.9.2
 */
public class NetworkDispatchException extends NetworkApiException {

    public NetworkDispatchException(@Nullable String source, String message, Throwable cause) {
        super(source, message, cause);
    }

    public NetworkDispatchException(@Nullable String source, String message) {
        super(source, message);
    }
}
