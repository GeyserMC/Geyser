/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.pack.option;

import org.geysermc.geyser.api.GeyserApi;

/**
 * Can be used for resource packs using the {@link org.geysermc.geyser.api.pack.UrlPackCodec}.
 * When a Bedrock client is unable to download a resource pack from a URL, Geyser will, by default,
 * serve the resource pack over raknet (as packs are served with the {@link org.geysermc.geyser.api.pack.PathPackCodec}).
 * This option can be used to disable that behavior, and disconnect the player instead.
 * By default, {@link UrlFallbackOption#TRUE} is set.
 */
public interface UrlFallbackOption extends ResourcePackOption<Boolean> {

    UrlFallbackOption TRUE = fallback(true);
    UrlFallbackOption FALSE = fallback(false);

    /**
     * Whether to fall back to serving packs over the raknet connection
     * @param fallback whether to fall back
     * @return a UrlFallbackOption with the specified behavior
     */
    static UrlFallbackOption fallback(boolean fallback) {
        return GeyserApi.api().provider(UrlFallbackOption.class, fallback);
    }

}
