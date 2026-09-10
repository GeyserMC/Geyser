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

/**
 * Host observations are independent of the publicly advertised server-list status.
 */
public final class ProviderRuntimeObservations {
    private ProviderRuntimeObservations() {
    }

    public static ProviderClient.Health health(int connectedPlayers, int capacity, long sampledAt, String build) {
        return new ProviderClient.Health(true, capacity,
                Math.min(1, (double) connectedPlayers / Math.max(1, capacity)), "nethernet", build,
                new ProviderClient.PlayerCount(connectedPlayers, sampledAt));
    }

    public static String registrationMessage(JsonObject registration) {
        String instanceId = registration.get("instanceId").getAsString();
        var address = registration.get("publicAddress");
        return address != null && !address.isJsonNull()
                ? "Provider address: " + address.getAsString() + " (instance " + instanceId + ")"
                : "Provider instance registered: " + instanceId + "; public addresses are managed on its attached Signal Servers";
    }
}
