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

package org.geysermc.geyser.network.bedrock.nethernet.signalling;

import com.google.gson.JsonParser;
import dev.kastle.netty.channel.nethernet.admission.AdmissionGate;
import io.netty.bootstrap.ServerBootstrap;
import org.cloudburstmc.netty.signalling.admission.EndpointAddress;
import org.cloudburstmc.netty.signalling.admission.NativeProviderTransport;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderEndpoint;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderHostFactory;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderHostIdentity;

import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Fixed native endpoint using the extension's existing Bedrock child pipeline.
 */
public final class NativeProviderHostFactory implements ProviderHostFactory {
    @Override
    public CompletionStage<Host> open(ServerBootstrap bootstrap, InetSocketAddress udpBind, Map<String, String> options) {
        try {
            String directory = options.get("stateDirectory");
            if (directory == null || directory.isBlank())
                throw new IllegalArgumentException("Provider stateDirectory required");
            Path state = Path.of(directory);
            List<InetSocketAddress> external = new ArrayList<>();
            for (var value : JsonParser.parseString(options.getOrDefault("advertisedEndpoints", "[]")).getAsJsonArray()) {
                var address = value.getAsJsonObject();
                external.add(new InetSocketAddress(EndpointAddress.parse(address.get("address").getAsString()), address.get("port").getAsInt()));
            }
            boolean localDevelopment = Boolean.parseBoolean(options.getOrDefault("localDevelopment", "false"));
            ProviderEndpoint endpoint = ProviderEndpoint.resolve(udpBind, external, localDevelopment);
            var identity = ProviderHostIdentity.ensure(state);
            return NativeProviderTransport.open(bootstrap, endpoint.bind(), () -> {
                                try {
                                    return ProviderEndpoint.resolve(udpBind, external, localDevelopment).advertised();
                                } catch (java.io.IOException unavailable) {
                                    throw new UncheckedIOException(unavailable);
                                }
                            },
                            identity.certificate(), identity.privateKey(), AdmissionGate.Limits.defaults())
                    .thenApply(transport -> new Host(transport, transport.channel(), endpoint.warnings()));
        } catch (Exception invalid) {
            return CompletableFuture.failedFuture(invalid);
        }
    }
}
