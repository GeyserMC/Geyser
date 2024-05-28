/*
 * Copyright (c) 2023 GeyserMC. http://geysermc.org
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

/**
 * Called on Geyser's startup when looking for custom skulls. Custom skulls must be registered through this event.
 * <p>
 * This event will not be called if the "add-non-bedrock-items" setting is disabled in the Geyser config.
 */
public abstract class GeyserDefineCustomSkullsEvent implements Event {
    /**
     * The type of texture provided
     */
    public enum SkullTextureType {
        USERNAME,
        UUID,
        PROFILE,
        SKIN_HASH
    }

    /**
     * Registers the given username, UUID, base64 encoded profile, or skin hash as a custom skull blocks
     * @param texture the username, UUID, base64 encoded profile, or skin hash
     * @param type the type of texture provided
     */
    public abstract void register(@NonNull String texture, @NonNull SkullTextureType type);
}
