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

package org.geysermc.geyser.network.bedrock.nethernet.signalling.provider;

import com.google.gson.JsonObject;
import org.cloudburstmc.netty.signalling.ProviderClient;
import org.cloudburstmc.netty.signalling.admission.EndpointAddress;
import org.geysermc.geyser.configuration.GeyserConfig;

import java.io.IOException;
import java.net.URI;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

/**
 * Validated NXS settings, with listener and status defaults inherited from Geyser.
 */
public record ProviderRuntimeConfiguration(
        URI origin, Path stateDirectory, String authorizationToken, String region, String pool,
        Map<String, String> tags, String label, String bindAddress, int udpPort,
        List<InetSocketAddress> advertisedEndpoints, int capacity
) {
    public static ProviderRuntimeConfiguration resolve(GeyserConfig.SignallingConfig config, Path directory, String bedrockAddress, int webrtcPort, int maxPlayers) throws IOException {
        var nxs = config.nxs();
        URI origin;
        try {
            origin = URI.create(nxs.endpoint());
            org.cloudburstmc.netty.signalling.ProviderCrypto.origin(origin);
        } catch (RuntimeException invalid) {
            throw new IOException("nxs.endpoint must be an HTTPS origin (HTTP is allowed only on loopback)");
        }
        String token = token(nxs.token(), directory);
        Map<String, String> tags = new TreeMap<>(nxs.data());
        String region = tags.remove("region"), pool = tags.remove("pool");
        if (region != null || pool != null || !tags.isEmpty()) {
            if (region == null) region = "global";
            if (pool == null) pool = "default";
        }
        Path state = directory.resolve("provider-state");
        // The WebRTC port: the Bedrock port itself, unless RakNet also runs
        String bind = bedrockAddress;
        int port = webrtcPort;
        if (port < 1 || port > 65535)
            throw new IOException("NXS needs a fixed UDP port between 1 and 65535");
        Set<InetSocketAddress> endpoints = new LinkedHashSet<>();
        for (String address : nxs.advertiseAddresses()) endpoints.add(endpoint(address));
        if (endpoints.size() > 32) throw new IOException("nxs.advertise-addresses allows at most 32 endpoints");
        int capacity = Math.max(1, maxPlayers);
        if (capacity < 1 || capacity > 1000000) throw new IOException("Invalid inherited routing capacity");
        String label = "Geyser";
        var runtime = new ProviderRuntimeConfiguration(origin, state, token, region, pool, Map.copyOf(tags), label,
                bind, port, List.copyOf(endpoints), capacity);
        try {
            runtime.clientConfiguration();
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid nxs.data: region/pool and tag names or values exceed the NXS limits");
        }
        return runtime;
    }

    public String profile() {
        return "nxs-admission-v1";
    }

    public ProviderClient.Configuration clientConfiguration() {
        return new ProviderClient.Configuration(origin, profile(), label, ProviderClient.AUTOMATIC,
                authorizationToken == null ? ProviderClient.ANONYMOUS_PROOF_OF_WORK : ProviderClient.BEARER_TOKEN,
                authorizationToken, region, pool, tags);
    }

    private static InetSocketAddress endpoint(String value) throws IOException {
        try {
            var match = java.util.regex.Pattern.compile("(?:\\[([^\\]]+)\\]|([^:]+)):([0-9]{1,5})").matcher(value);
            if (!match.matches()) throw new IllegalArgumentException();
            int port = Integer.parseInt(match.group(3));
            if (port < 1 || port > 65535) throw new IllegalArgumentException();
            var address = EndpointAddress.parse(match.group(1) == null ? match.group(2) : match.group(1));
            if (address.isAnyLocalAddress() || address.isMulticastAddress() || address.isLinkLocalAddress())
                throw new IllegalArgumentException();
            return new InetSocketAddress(address, port);
        } catch (Exception invalid) {
            throw new IOException("nxs.advertise-addresses entries must be numeric IPv4:port or [IPv6]:port with ports 1-65535");
        }
    }

    private static String token(String value, Path directory) throws IOException {
        if (value == null || value.isBlank()) return null;
        value = value.trim();
        boolean file = value.startsWith("file:") || value.startsWith("/") || value.startsWith("./") || value.startsWith("../") || absolutePath(value);
        if (file) {
            try {
                Path source = path(directory, value.startsWith("file:") ? value.substring(5) : value);
                if (!Files.isRegularFile(source) || Files.size(source) > 16384) throw new IOException();
                value = Files.readString(source).trim();
            } catch (Exception invalid) {
                throw new IOException("nxs.token file must be readable and contain at most 16384 bytes");
            }
        }
        if (value.isBlank() || value.length() > 16384 || value.chars().anyMatch(c -> c <= 32 || c == 127))
            throw new IOException("nxs.token must contain one non-empty bearer token");
        return value;
    }

    /**
     * @return whether the value is an absolute path, e.g. C:\token on Windows, so it is never sent as the token itself
     */
    private static boolean absolutePath(String value) {
        try {
            return Path.of(value).isAbsolute();
        } catch (InvalidPathException notAPath) {
            return false;
        }
    }

    private static Path path(Path directory, String value) {
        Path path = Path.of(value);
        return (path.isAbsolute() ? path : directory.resolve(path)).normalize();
    }

    public String encodedAdvertisedEndpoints() {
        var values = new com.google.gson.JsonArray();
        for (var endpoint : advertisedEndpoints) {
            var value = new JsonObject();
            value.addProperty("address", endpoint.getAddress().getHostAddress());
            value.addProperty("port", endpoint.getPort());
            values.add(value);
        }
        return values.toString();
    }

    @Override
    public String toString() {
        return "ProviderRuntimeConfiguration[origin=" + origin + ", profile=" + profile() + "]";
    }
}
