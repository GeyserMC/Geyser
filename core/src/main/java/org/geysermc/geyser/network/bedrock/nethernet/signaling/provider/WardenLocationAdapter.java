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

package org.geysermc.geyser.network.bedrock.nethernet.signaling.provider;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Optional Warden metadata carried by the neutral NXS extension envelope. */
public final class WardenLocationAdapter {
    private WardenLocationAdapter() {
    }

    public static JsonObject extensions(Map<String, String> settings) throws IOException {
        JsonObject location = new JsonObject();
        try {
            if (!Set.of("country", "city", "latitude", "longitude").containsAll(settings.keySet())) {
                throw new IllegalArgumentException();
            }
            if (settings.containsKey("country")) {
                String country = settings.get("country").trim().toUpperCase(Locale.ROOT);
                if (!country.matches("[A-Z]{2}") || Set.of("XX", "ZZ").contains(country)) {
                    throw new IllegalArgumentException();
                }
                location.addProperty("country", country);
            }
            if (settings.containsKey("city")) {
                String city = settings.get("city").trim();
                if (city.isEmpty() || city.length() > 120 || city.codePoints().anyMatch(codePoint ->
                    Character.isISOControl(codePoint) || Character.getType(codePoint) == Character.SURROGATE)) {
                    throw new IllegalArgumentException();
                }
                location.addProperty("city", city);
            }
            if (settings.containsKey("latitude") != settings.containsKey("longitude")) {
                throw new IllegalArgumentException();
            }
            if (settings.containsKey("latitude")) {
                double latitude = Double.parseDouble(settings.get("latitude"));
                double longitude = Double.parseDouble(settings.get("longitude"));
                if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
                    throw new IllegalArgumentException();
                }
                location.addProperty("latitude", latitude);
                location.addProperty("longitude", longitude);
            }
            if (!settings.isEmpty() && !location.has("country") && !location.has("latitude")) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException invalid) {
            throw new IOException("nxs.location requires a country code and/or a valid latitude/longitude pair; city is optional");
        }
        JsonObject data = new JsonObject();
        JsonObject extension = new JsonObject();
        JsonObject extensions = new JsonObject();
        data.add("location", location.isEmpty() ? JsonNull.INSTANCE : location);
        extension.addProperty("version", 1);
        extension.addProperty("critical", false);
        extension.add("data", data);
        extensions.add("cloud.warden.location", extension);
        return extensions;
    }
}
