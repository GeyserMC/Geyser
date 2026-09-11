/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network;

import org.cloudburstmc.netty.util.nethernet.IpRangeSet;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.util.WebUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The proxies allowed to speak for a client, from
 * {@link org.geysermc.geyser.configuration.GeyserConfig.AdvancedBedrockConfig#haproxyProtocolWhitelistedIps()}.
 * <p>
 * Shared by the RakNet and signalling listeners so both honor the same addresses, and resolved once
 * per start because an entry may be a URL that has to be fetched.
 */
public final class ProxyWhitelist {

    private static IpRangeSet resolved;

    private ProxyWhitelist() {
    }

    /**
     * @param geyser the running instance, for its configuration
     * @return the whitelisted addresses, empty when nothing is configured
     */
    public static synchronized IpRangeSet get(GeyserImpl geyser) {
        IpRangeSet cached = resolved;
        if (cached != null) {
            return cached;
        }

        List<String> entries = new ArrayList<>();
        for (String entry : geyser.config().advanced().bedrock().haproxyProtocolWhitelistedIps()) {
            if (!entry.startsWith("http")) {
                entries.add(entry);
                continue;
            }

            WebUtils.getLineStream(entry).forEach(entries::add);
        }
        return resolved = IpRangeSet.parse(entries);
    }

    /**
     * Forgets the resolved list, so a reload picks up a changed configuration.
     */
    public static synchronized void invalidate() {
        resolved = null;
    }
}
