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

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Optional Warden ownership actions; no claim behavior belongs to the NXS client.
 */
public final class WardenClaimAdapter {
    public static final String NAMESPACE = "cloud.warden.claim";
    private final Supplier<? extends CompletionStage<JsonObject>> extensions;
    private final Supplier<? extends CompletionStage<JsonObject>> readinessExtensions;
    private final Supplier<? extends CompletionStage<JsonObject>> refresh;
    private final LongSupplier now;

    public record Action(URI url, long expiresAt, String text) {
        public String message() {
            return text + " " + url;
        }
    }

    public WardenClaimAdapter(ProviderClient client) {
        this(client::extensions,
            () -> client.readiness().thenApply(WardenClaimAdapter::extensionsFrom),
            () -> client.extensionRequest(NAMESPACE, "refresh", "POST", new JsonObject()),
            System::currentTimeMillis);
    }

    WardenClaimAdapter(Supplier<? extends CompletionStage<JsonObject>> extensions,
                       Supplier<? extends CompletionStage<JsonObject>> readinessExtensions,
                       Supplier<? extends CompletionStage<JsonObject>> refresh, LongSupplier now) {
        this.extensions = extensions;
        this.readinessExtensions = readinessExtensions;
        this.refresh = refresh;
        this.now = now;
    }

    public CompletionStage<Optional<Action>> current() {
        return extensions.get().thenApply(metadata -> action(metadata, now.getAsLong()));
    }

    /**
     * Called only by the operator's explicit console command.
     */
    public CompletionStage<Optional<Action>> refresh() {
        // Recovery omits private action URLs. Readiness can still advertise availability.
        return readinessExtensions.get().thenCompose(metadata -> {
            var data = data(metadata);
            if (data.isEmpty() || !available(data.get())) return CompletableFuture.completedFuture(Optional.empty());
            return refresh.get().thenApply(response -> action(extensionsFrom(response), now.getAsLong()));
        });
    }

    private static JsonObject extensionsFrom(JsonObject response) {
        return response.has("extensions") && response.get("extensions").isJsonObject()
            ? response.getAsJsonObject("extensions") : new JsonObject();
    }

    private static boolean available(JsonObject data) {
        return !data.has("available") || (data.get("available").isJsonPrimitive()
            && data.getAsJsonPrimitive("available").isBoolean() && data.get("available").getAsBoolean());
    }

    private static Optional<JsonObject> data(JsonObject metadata) {
        try {
            JsonObject extension = metadata.getAsJsonObject(NAMESPACE);
            if (extension == null || !extension.get("version").isJsonPrimitive()
                || !extension.getAsJsonPrimitive("version").isNumber() || !extension.get("version").getAsString().equals("1")
                || !extension.get("critical").isJsonPrimitive()
                || !extension.getAsJsonPrimitive("critical").isBoolean()
                || extension.get("critical").getAsBoolean()) return Optional.empty();
            return Optional.of(extension.getAsJsonObject("data"));
        } catch (RuntimeException malformed) {
            return Optional.empty();
        }
    }

    static Optional<Action> action(JsonObject metadata, long now) {
        return data(metadata).flatMap(data -> {
            try {
                if (!available(data)) return Optional.empty();
                JsonObject action = data.getAsJsonObject("action");
                if (action == null || !action.get("url").isJsonPrimitive()
                    || !action.getAsJsonPrimitive("url").isString()
                    || !action.getAsJsonPrimitive("text").isString()
                    || !action.getAsJsonPrimitive("expiresAt").isNumber()
                    || !action.get("expiresAt").getAsString().matches("[1-9][0-9]{0,15}")) return Optional.empty();
                String url = action.get("url").getAsString(), text = action.get("text").getAsString();
                long expiresAt = action.get("expiresAt").getAsLong();
                if (expiresAt <= now || url.length() > 2048 || text.isBlank() || text.length() > 500
                    || text.codePoints().anyMatch(Character::isISOControl)) return Optional.empty();
                URI uri = URI.create(url);
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getFragment() != null) return Optional.empty();
                return Optional.of(new Action(uri, expiresAt, text));
            } catch (RuntimeException malformed) {
                return Optional.empty();
            }
        });
    }
}
