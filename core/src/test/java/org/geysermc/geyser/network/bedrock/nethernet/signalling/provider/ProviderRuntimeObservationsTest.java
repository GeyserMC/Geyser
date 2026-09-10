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

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProviderRuntimeObservationsTest {
    @Test void acceptsFleetRegistrationsWithoutAnOriginatingPublicAddress() {
        var registration = JsonParser.parseString("{\"instanceId\":\"game-one\"}").getAsJsonObject();
        assertTrue(ProviderRuntimeObservations.registrationMessage(registration).contains("game-one"));
        registration.add("publicAddress", com.google.gson.JsonNull.INSTANCE);
        assertTrue(ProviderRuntimeObservations.registrationMessage(registration).contains("attached Signal Servers"));
        registration.addProperty("publicAddress", "https://play.example");
        assertEquals("Provider address: https://play.example (instance game-one)", ProviderRuntimeObservations.registrationMessage(registration));
    }

    @Test void reportsActualPlayersEvenWhenAdmissionCapacityHasBeenReduced() {
        var health = ProviderRuntimeObservations.health(143, 100, 123456789L, "test");
        assertEquals(143, health.playerCount().connectedPlayers());
        assertEquals(123456789L, health.playerCount().sampledAt());
        assertEquals(100, health.capacity());
        assertEquals(1, health.load());
        assertEquals(0, ProviderRuntimeObservations.health(0, 100, 123456790L, "test").playerCount().connectedPlayers());
    }
}
