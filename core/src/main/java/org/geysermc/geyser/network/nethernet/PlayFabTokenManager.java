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

package org.geysermc.geyser.network.nethernet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Handles PlayFab authentication and MCToken acquisition.
 * The MCToken is required for Nethernet signaling WebSocket authentication.
 *
 * Auth chain: PlayFab LoginWithCustomID -> SessionTicket -> session/start -> MCToken
 */
public class PlayFabTokenManager {

    private static final String PLAYFAB_BASE = "https://6955f.playfabapi.com";
    private static final String SESSION_START_URL = "https://authorization.franchise.minecraft-services.net/api/v1.0/session/start";
    private static final String TITLE_ID = "6955F";
    private static final int HTTP_TIMEOUT = 15000;
    /** Cached tokens within this margin of expiry are treated as expired. */
    private static final long TOKEN_EXPIRY_MARGIN_MILLIS = 10 * 60 * 1000;

    private final GeyserLogger logger;
    private final String customId;
    private final String deviceId;

    private volatile @Nullable String sessionTicket;
    private volatile @Nullable String mcToken;
    private volatile @Nullable String mcTokenExpiry;
    private volatile @Nullable String pmsgId;

    /**
     * The customId and deviceId come persisted from connection-id.yml: the pmid
     * is bound to the anonymous PlayFab account behind the customId, so reusing
     * the same identity keeps the connection ID clients type stable across
     * restarts.
     */
    public PlayFabTokenManager(GeyserLogger logger, String customId, String deviceId) {
        this.logger = logger;
        this.customId = customId;
        this.deviceId = deviceId;
    }

    /**
     * Returns a valid MCToken, reusing the cached one while it is still well
     * within its validity window and performing the full auth chain
     * (PlayFab login -> MCToken acquisition) otherwise. Frequent signaling
     * reconnects therefore do not hammer PlayFab. If the service rejects a
     * token that looks valid by expiry, call {@link #invalidate()} first to
     * force a full re-auth.
     *
     * @return the MCToken authorization header (e.g. "MCToken eyJ..."), or null on failure
     */
    public @Nullable String authenticate() {
        String cached = mcToken;
        if (cached != null && tokenStillValid()) {
            return cached;
        }
        try {
            loginToPlayFab();
            obtainMCToken();
            return mcToken;
        } catch (Exception e) {
            logger.error("[Nethernet] PlayFab authentication failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Drops the cached token so the next {@link #authenticate()} performs a
     * full re-auth. Used when the signaling service refuses a token that has
     * not yet reached its recorded expiry.
     */
    public void invalidate() {
        mcToken = null;
        mcTokenExpiry = null;
        sessionTicket = null;
    }

    private boolean tokenStillValid() {
        String expiry = mcTokenExpiry;
        if (expiry == null) {
            return false;
        }
        try {
            long expiresAt;
            try {
                expiresAt = Instant.parse(expiry).toEpochMilli();
            } catch (Exception e) {
                expiresAt = OffsetDateTime.parse(expiry).toInstant().toEpochMilli();
            }
            return expiresAt - TOKEN_EXPIRY_MARGIN_MILLIS > System.currentTimeMillis();
        } catch (Exception e) {
            // Unparseable expiry: play it safe and re-auth.
            return false;
        }
    }

    public @Nullable String getMCToken() {
        return mcToken;
    }

    public @Nullable String getPmsgId() {
        return pmsgId;
    }

    private @Nullable String extractPmsgId(String token) {
        // MCToken format: "MCToken eyJ..." — decode the JWT payload
        String[] parts = token.split(" ", 2);
        if (parts.length < 2) return null;
        String[] jwtParts = parts[1].split("\\.");
        if (jwtParts.length < 2) return null;
        try {
            String payload = new String(java.util.Base64.getUrlDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8);
            JsonObject claims = JsonParser.parseString(payload).getAsJsonObject();
            return claims.has("pmid") ? claims.get("pmid").getAsString() : null;
        } catch (Exception e) {
            logger.debug("[Nethernet] Failed to extract pmid from MCToken: " + e.getMessage());
            return null;
        }
    }

    private void loginToPlayFab() throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("CreateAccount", true);
        payload.addProperty("CustomId", customId);
        payload.add("EncryptedRequest", null);

        JsonObject infoParams = new JsonObject();
        infoParams.addProperty("GetCharacterInventories", false);
        infoParams.addProperty("GetCharacterList", false);
        infoParams.addProperty("GetPlayerProfile", true);
        infoParams.addProperty("GetPlayerStatistics", false);
        infoParams.addProperty("GetTitleData", false);
        infoParams.addProperty("GetUserAccountInfo", true);
        infoParams.addProperty("GetUserData", false);
        infoParams.addProperty("GetUserInventory", false);
        infoParams.addProperty("GetUserReadOnlyData", false);
        infoParams.addProperty("GetUserVirtualCurrency", false);
        payload.add("InfoRequestParameters", infoParams);
        payload.add("PlayerSecret", null);
        payload.addProperty("TitleId", TITLE_ID);

        JsonObject response = postJson(PLAYFAB_BASE + "/Client/LoginWithCustomID", payload.toString(), null);

        if (!response.has("data") || !response.getAsJsonObject("data").has("SessionTicket")) {
            throw new IOException("PlayFab login response missing SessionTicket");
        }

        this.sessionTicket = response.getAsJsonObject("data").get("SessionTicket").getAsString();
        logger.debug("[Nethernet] PlayFab login successful");
    }

    private void obtainMCToken() throws IOException {
        if (sessionTicket == null) {
            throw new IOException("No PlayFab session ticket available");
        }

        JsonObject device = new JsonObject();
        device.addProperty("applicationType", "MinecraftPE");
        device.add("capabilities", null);
        device.addProperty("gameVersion", "1.21.10");
        device.addProperty("id", deviceId);
        device.addProperty("memory", "2147483647");
        device.addProperty("platform", "Win32");
        device.addProperty("playFabTitleId", TITLE_ID);
        device.addProperty("storePlatform", "uwp.store");
        device.add("treatmentOverrides", null);
        device.addProperty("type", "Win32");

        JsonObject user = new JsonObject();
        user.addProperty("language", "en");
        user.addProperty("languageCode", "en-US");
        user.addProperty("regionCode", "US");
        user.addProperty("token", sessionTicket);
        user.addProperty("tokenType", "PlayFab");

        JsonObject payload = new JsonObject();
        payload.add("device", device);
        payload.add("user", user);

        JsonObject response = postJson(SESSION_START_URL, payload.toString(), "libhttpclient/1.0.0.0");

        if (!response.has("result") || !response.getAsJsonObject("result").has("authorizationHeader")) {
            throw new IOException("session/start response missing authorizationHeader");
        }

        JsonObject result = response.getAsJsonObject("result");
        this.mcToken = result.get("authorizationHeader").getAsString();
        if (result.has("validUntil")) {
            this.mcTokenExpiry = result.get("validUntil").getAsString();
        }

        // Extract pmid from the MCToken JWT payload
        this.pmsgId = extractPmsgId(this.mcToken);

        logger.debug("[Nethernet] MCToken obtained");
    }

    private JsonObject postJson(String url, String jsonBody, @Nullable String userAgent) throws IOException {
        HttpURLConnection con = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            if (userAgent != null) {
                con.setRequestProperty("User-Agent", userAgent);
            }
            con.setConnectTimeout(HTTP_TIMEOUT);
            con.setReadTimeout(HTTP_TIMEOUT);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = con.getResponseCode();
            if (code >= 400) {
                String err = readStream(con.getErrorStream());
                throw new IOException("HTTP " + code + ": " + err);
            }

            try (InputStreamReader isr = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(isr).getAsJsonObject();
            }
        } finally {
            con.disconnect();
        }
    }

    private String readStream(@Nullable InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
