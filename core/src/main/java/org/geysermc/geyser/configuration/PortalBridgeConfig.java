/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.configuration;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ConfigSerializable
public final class PortalBridgeConfig {
    @Comment("""
        Whether to enable the NetherNet portal ingress.
        The normal Bedrock UDP listener remains available for direct server connections.""")
    private boolean enabled;

    @Comment("""
        A list of trusted proxy IP addresses or CIDR ranges that are allowed to send SELF_SIGNED Bedrock logins
        into Geyser for portal bridge sessions.
        Keep this empty unless you control the ingress adapter and have blocked direct public access to this
        Geyser instance.""")
    private List<String> trustedProxyIps;

    @Comment("""
        Whether to emit extra portal bridge startup logging.
        Packet-level debug logging still uses the JVM property Geyser.ProxyBridgeDebug.""")
    private boolean debugLogging;

    @Comment("""
        Optional explicit NetherNet network id to bind to.
        Leave empty to let the signaling layer choose one, or set it when you need the bridge to keep a stable Xbox/NetherNet identity across restarts.""")
    private String netherNetNetworkId;

    @Comment("""
        The number of parallel NetherNet ingress IDs to expose.
        Each ID can be published as its own Xbox session shard while still landing in the same Geyser backend.""")
    private int shardCount = 1;

    @Comment("""
        Xbox/NetherNet authorization header for the server-side signaling session.
        This is currently required to terminate NetherNet sessions directly inside Geyser until Xbox session management is migrated here too.""")
    private String xboxAuthHeader;

    @Comment("""
        Optional path to an MCXboxBroadcast cache.json file.
        If xbox-auth-header is empty, Geyser will read minecraftSession.authorizationHeader from this file at startup.""")
    private String xboxAuthHeaderFile;

    public boolean enabled() {
        return enabled;
    }

    public List<String> trustedProxyIps() {
        return Objects.requireNonNullElse(trustedProxyIps, Collections.emptyList());
    }

    public boolean debugLogging() {
        return debugLogging;
    }

    public String netherNetNetworkId() {
        return Objects.requireNonNullElse(netherNetNetworkId, "");
    }

    public int shardCount() {
        return Math.max(1, shardCount);
    }

    public String xboxAuthHeader() {
        return Objects.requireNonNullElse(xboxAuthHeader, "");
    }

    public String xboxAuthHeaderFile() {
        return Objects.requireNonNullElse(xboxAuthHeaderFile, "");
    }
}
