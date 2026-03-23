/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fully-automated Education Edition authentication manager using Microsoft's admin tooling API.
 *
 * <p>Uses the admin tooling app ID ({@code 1c91b067-...}) with OAuth v2.0 endpoints and
 * PascalCase field conventions from the official MESS tooling notebook. This enables
 * complete server automation in a single device code login:
 * <ol>
 *   <li>Enable tenant settings (dedicated servers, teacher access)</li>
 *   <li>Register or fetch the server</li>
 *   <li>Host the server (register IP with MESS)</li>
 *   <li>Configure, enable, and broadcast the server</li>
 * </ol>
 *
 */
public class EducationAuthManager {

    // Admin Tooling app ID from Microsoft's official MESS tooling notebook.
    // Used for tooling/* endpoints (edit_tenant_settings, edit_server_info, etc.)
    private static final String TOOLING_CLIENT_ID = "1c91b067-6806-44a5-8d2d-3137e625f5b8";

    // Education Client app ID - required for server/* endpoints (register, fetch_token).
    // The tooling app ID is NOT accepted by server/* endpoints (returns 401).
    private static final String EDU_CLIENT_ID = "b36b1432-1a1c-4c82-9b76-24de1cab42f2";

    // MESS resource ID with /.default scope suffix (v2.0 OAuth format)
    private static final String SCOPE = "16556bfc-5102-43c9-a82a-3ea5e4810689/.default offline_access";

    // MESS resource ID (bare, for v1.0 OAuth resource= parameter)
    private static final String MESS_RESOURCE = "16556bfc-5102-43c9-a82a-3ea5e4810689";

    // OAuth v2.0 endpoints under /organizations (restricts to work/school accounts)
    private static final String ENTRA_BASE = "https://login.microsoftonline.com/organizations/oauth2/v2.0";
    // OAuth v1.0 endpoints under /common (any M365 account)
    private static final String ENTRA_V1_BASE = "https://login.microsoftonline.com/common/oauth2";
    private static final String MESS_BASE = "https://dedicatedserver.minecrafteduservices.com";
    // Discovery endpoint for standalone token exchange
    private static final String DISCOVERY_BASE = "https://discovery.minecrafteduservices.com";

    /**
     * Server health status for MESS /server/update heartbeats.
     * MESS accepts values -1 to 2 (Microsoft docs incorrectly claim 0-3; 3 is rejected).
     * 2 = optimal/healthy.
     */
    private static final int MESS_HEALTH_OPTIMAL = 2;
    private static final String OFFICIAL_FILE = "edu_official.yml";
    private static final String STANDALONE_FILE = "edu_standalone.yml";
    private static final String LOG_PREFIX = "[EduTooling] ";
    private static final int HTTP_TIMEOUT = 15000;
    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 60;

    private @Nullable GeyserImpl geyser;
    private @Nullable GeyserLogger logger;
    private @Nullable String serverId;
    private @Nullable String serverIp;
    private @Nullable String serverName;
    private int maxPlayers;

    // Tooling token state (tooling app ID - for tooling/* endpoints)
    private volatile @Nullable String refreshToken;
    private volatile @Nullable String accessToken;
    private volatile long accessTokenExpires;

    // Edu client token state (edu client app ID - for server/register, server/fetch_token)
    private volatile @Nullable String eduRefreshToken;
    private volatile @Nullable String eduAccessToken;
    private volatile long eduAccessTokenExpires;

    // MESS server token (obtained via server/register or server/fetch_token)
    private volatile @Nullable String serverToken;      // Inner payload token (for handshake signedToken)
    private volatile @Nullable String serverTokenJwt;    // Full JWT string (kept for session persistence and token refresh)
    private volatile long serverTokenExpires;

    // Multi-tenancy: maps tenant ID to list of server tokens for routing.
    // Multiple tokens per tenant are allowed; at routing time the newest (by expiry timestamp) is selected.
    private final ConcurrentHashMap<String, List<String>> tenantTokenPool = new ConcurrentHashMap<>();
    // Tenant IDs that came from config (server-tokens), as opposed to MESS registration
    private final ConcurrentHashMap.KeySetView<String, Boolean> configTrustTenants = ConcurrentHashMap.newKeySet();

    private final Object sessionFileLock = new Object();
    private volatile ScheduledFuture<?> updateTask;
    private volatile ScheduledFuture<?> tokenRefreshTask;
    private volatile boolean shutdownRequested;

    // Standalone device-code tokens: each entry holds the refresh token and server token for one tenant
    private final CopyOnWriteArrayList<StandaloneTokenEntry> standaloneTokens = new CopyOnWriteArrayList<>();
    private volatile ScheduledFuture<?> standaloneRefreshTask;
    private volatile boolean standaloneFlowInProgress;

    // Nonce verification cache: joinerToHostNonce → NonceEntry
    private static final long NONCE_EXPIRY_MS = 30 * 60 * 1000L; // 30 minutes
    private static final int NONCE_FETCH_TIMEOUT = 3000; // 3 seconds
    private final ConcurrentHashMap<String, NonceEntry> nonceCache = new ConcurrentHashMap<>();
    private final AtomicLong verifiedJoins = new AtomicLong();
    private final AtomicLong unverifiedJoins = new AtomicLong();
    private final AtomicLong rejectedJoins = new AtomicLong();

    record NonceEntry(String sessionToken, long fetchedAt) {}

    private @Nullable Path officialFilePath;
    private @Nullable Path standaloneFilePath;

    /**
     * Sets up core fields (geyser, logger, file paths) needed by all modes.
     * Generates edu_official.yml and edu_standalone.yml if they don't exist.
     * Must be called before any other method. Does not start MESS registration.
     */
    public void setup(GeyserImpl geyser) {
        this.geyser = geyser;
        this.logger = geyser.getLogger();
        this.officialFilePath = geyser.configDirectory().resolve(OFFICIAL_FILE);
        this.standaloneFilePath = geyser.configDirectory().resolve(STANDALONE_FILE);

        generateOfficialFile();
        generateStandaloneFile();
    }

    /**
     * Generates edu_official.yml with config fields at the top and session data below.
     */
    private void generateOfficialFile() {
        if (Files.exists(officialFilePath)) {
            return;
        }
        try {
            Files.writeString(officialFilePath, buildOfficialYaml("", "", 40, "", null, null, 0, null, null, 0, null, null, 0));
            logger.debug(LOG_PREFIX + "Generated " + OFFICIAL_FILE);
        } catch (IOException e) {
            logger.error(LOG_PREFIX + "Failed to generate " + OFFICIAL_FILE + ": " + e.getMessage());
        }
    }

    private static String buildOfficialYaml(String serverName, String serverIp, int maxPlayers,
                                            String serverId, @Nullable String refreshToken, @Nullable String accessToken,
                                            long accessTokenExpires, @Nullable String eduRefreshToken,
                                            @Nullable String eduAccessToken, long eduAccessTokenExpires,
                                            @Nullable String serverToken, @Nullable String serverTokenJwt,
                                            long serverTokenExpires) {
        return "# ============================================================\n" +
            "# EduGeyser Official Server Configuration\n" +
            "# ============================================================\n" +
            "# Configure these fields for official/hybrid tenancy mode.\n" +
            "# This file is also used to store MESS session data (below the config section).\n\n" +
            "# Display name for the server in Education Edition's server list.\n" +
            "server-name: \"" + escapeYaml(serverName) + "\"\n\n" +
            "# Public IP:port for MESS registration (e.g. \"mc.example.com:19132\").\n" +
            "# If empty, Geyser will attempt to auto-detect your public IP.\n" +
            "# Recommended to set this manually, especially behind NAT or proxies.\n" +
            "server-ip: \"" + escapeYaml(serverIp) + "\"\n\n" +
            "# Maximum players shown in the Education Edition server list.\n" +
            "max-players: " + maxPlayers + "\n\n" +
            "# --- Session data (managed automatically, do not edit below this line) ---\n" +
            "server-id: \"" + escapeYaml(serverId) + "\"\n" +
            "refresh-token: " + yamlStringOrNull(refreshToken) + "\n" +
            "access-token: " + yamlStringOrNull(accessToken) + "\n" +
            "access-token-expires: " + accessTokenExpires + "\n" +
            "edu-refresh-token: " + yamlStringOrNull(eduRefreshToken) + "\n" +
            "edu-access-token: " + yamlStringOrNull(eduAccessToken) + "\n" +
            "edu-access-token-expires: " + eduAccessTokenExpires + "\n" +
            "server-token: " + yamlStringOrNull(serverToken) + "\n" +
            "server-token-jwt: " + yamlStringOrNull(serverTokenJwt) + "\n" +
            "server-token-expires: " + serverTokenExpires + "\n";
    }

    /**
     * Generates edu_standalone.yml with token paste slots and device-code section.
     */
    private void generateStandaloneFile() {
        if (Files.exists(standaloneFilePath)) {
            return;
        }
        try {
            Files.writeString(standaloneFilePath, buildStandaloneYaml(
                List.of("", "", ""), List.of()
            ));
            logger.debug(LOG_PREFIX + "Generated " + STANDALONE_FILE);
        } catch (IOException e) {
            logger.error(LOG_PREFIX + "Failed to generate " + STANDALONE_FILE + ": " + e.getMessage());
        }
    }

    private static String buildStandaloneYaml(List<String> manualTokens,
                                               List<StandaloneTokenEntry> deviceCodeTokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ============================================================\n");
        sb.append("# EduGeyser Standalone Token Configuration\n");
        sb.append("# ============================================================\n");
        sb.append("# Paste server tokens from the EduGeyser Token Tool here.\n");
        sb.append("# Each token authorizes one school's tenant.\n");
        sb.append("tokens:\n");
        for (String token : manualTokens) {
            sb.append("  - \"").append(escapeYaml(token)).append("\"\n");
        }
        sb.append("\n# --- Device-code tokens (managed automatically, do not edit below this line) ---\n");
        if (deviceCodeTokens.isEmpty()) {
            sb.append("device-code-tokens: []\n");
        } else {
            sb.append("device-code-tokens:\n");
            for (StandaloneTokenEntry entry : deviceCodeTokens) {
                sb.append("  - tenant-id: \"").append(escapeYaml(entry.tenantId)).append("\"\n");
                if (entry.refreshToken != null) {
                    sb.append("    refresh-token: \"").append(escapeYaml(entry.refreshToken)).append("\"\n");
                }
                sb.append("    server-token: \"").append(escapeYaml(entry.serverToken)).append("\"\n");
            }
        }
        return sb.toString();
    }

    /**
     * Reads config fields (server-name, server-ip, max-players, server-id) from edu_official.yml.
     */
    private void loadOfficialConfig() {
        if (!Files.exists(officialFilePath)) {
            return;
        }
        try {
            var loader = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                    .path(officialFilePath).build();
            var node = loader.load();
            this.serverName = node.node("server-name").getString("");
            this.serverIp = node.node("server-ip").getString("");
            this.serverId = node.node("server-id").getString("");
            this.maxPlayers = node.node("max-players").getInt(40);
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Failed to load " + OFFICIAL_FILE + ": " + e.getMessage(), e);
        }
    }

    /**
     * Saves session data back to edu_official.yml, preserving config fields and comments.
     * Reads current config values from file first, then rewrites the entire file.
     */
    private void saveOfficialSession() {
        synchronized (sessionFileLock) {
            if (officialFilePath == null) {
                return;
            }
            try {
                // Read current config values from file to preserve user edits
                String cfgServerName = serverName != null ? serverName : "";
                String cfgServerIp = serverIp != null ? serverIp : "";
                int cfgMaxPlayers = maxPlayers;
                if (Files.exists(officialFilePath)) {
                    var loader = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                            .path(officialFilePath).build();
                    var node = loader.load();
                    cfgServerName = node.node("server-name").getString("");
                    cfgServerIp = node.node("server-ip").getString("");
                    cfgMaxPlayers = node.node("max-players").getInt(40);
                }

                String sid = (serverId != null && !serverId.isEmpty()) ? serverId : "";
                Files.writeString(officialFilePath, buildOfficialYaml(
                    cfgServerName, cfgServerIp, cfgMaxPlayers, sid,
                    refreshToken, accessToken, accessTokenExpires,
                    eduRefreshToken, eduAccessToken, eduAccessTokenExpires,
                    serverToken, serverTokenJwt, serverTokenExpires
                ));
            } catch (Exception e) {
                logger.error(LOG_PREFIX + "Failed to save " + OFFICIAL_FILE + ": " + e.getMessage(), e);
            }
        }
    }

    private static String escapeYaml(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String yamlStringOrNull(@Nullable String value) {
        if (value == null) return "null";
        return "\"" + escapeYaml(value) + "\"";
    }

    /**
     * Loads session data from edu_official.yml (tokens, expiry, etc.).
     */
    private void loadOfficialSession() {
        if (!Files.exists(officialFilePath)) {
            return;
        }
        synchronized (sessionFileLock) {
            try {
                var loader = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                        .path(officialFilePath).build();
                var node = loader.load();
                this.refreshToken = node.node("refresh-token").getString();
                this.accessToken = node.node("access-token").getString();
                this.accessTokenExpires = node.node("access-token-expires").getLong(0);
                this.eduRefreshToken = node.node("edu-refresh-token").getString();
                this.eduAccessToken = node.node("edu-access-token").getString();
                this.eduAccessTokenExpires = node.node("edu-access-token-expires").getLong(0);
                this.serverToken = node.node("server-token").getString();
                this.serverTokenJwt = node.node("server-token-jwt").getString();
                this.serverTokenExpires = node.node("server-token-expires").getLong(0);
                String fileServerId = node.node("server-id").getString();
                if ((serverId == null || serverId.isEmpty()) && fileServerId != null && !fileServerId.isEmpty()) {
                    this.serverId = fileServerId;
                }
            } catch (Exception e) {
                logger.error(LOG_PREFIX + "Failed to load session from " + OFFICIAL_FILE + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Initializes the MESS registration flow (official/hybrid modes).
     * Starts the device code login and registers the server with MESS.
     * {@link #setup(GeyserImpl)} must be called first.
     */
    public void initialize() {
        loadOfficialConfig();

        boolean hasServerId = serverId != null && !serverId.isEmpty();
        boolean hasServerName = serverName != null && !serverName.isEmpty();

        if (!hasServerId && !hasServerName) {
            logger.debug(LOG_PREFIX + "No server-id or server-name configured in " + OFFICIAL_FILE + ". Education tooling auth manager inactive.");
            return;
        }

        logger.debug(LOG_PREFIX + "Initializing: serverId=%s, serverName=%s, maxPlayers=%s",
                serverId, serverName, maxPlayers);

        this.serverIp = resolveServerIp();

        // Run auth flow on the scheduled thread to avoid blocking startup
        geyser.getScheduledThread().execute(this::runAuthFlow);
    }

    private String resolveServerIp() {
        // serverIp is loaded from edu_official.yml in loadOfficialConfig()
        if (serverIp != null && !serverIp.isEmpty()) {
            return serverIp;
        }

        int port = geyser.config().bedrock().port();
        String detectedIp = detectPublicIp();
        if (detectedIp != null) {
            return detectedIp + ":" + port;
        }

        // Fallback to bind address
        String address = geyser.config().bedrock().address();
        if ("0.0.0.0".equals(address)) {
            address = "127.0.0.1";
        }
        String result = address + ":" + port;
        logger.warning(LOG_PREFIX + "Could not auto-detect public IP. Using bind address: " + result);
        logger.warning(LOG_PREFIX + "Set 'server-ip' in " + OFFICIAL_FILE + " to your public IP:port for external access.");
        return result;
    }

    // ---- Main Auth Flow ----

    private void runAuthFlow() {
        try {
            loadOfficialSession();
            restoreOrAuthenticate();
        } catch (InterruptedException e) {
            // Re-authentication was initiated asynchronously — not a failure
            logger.debug(LOG_PREFIX + e.getMessage());
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Authentication flow failed: " + e.getMessage(), e);
        }
    }

    /**
     * Completes the auth flow after tokens are available (either restored or
     * obtained via device code). Handles server registration, hosting, and
     * scheduling of periodic tasks.
     */
    private void completeAuthFlow() {
        try {
            if (serverId != null && !serverId.isEmpty()) {
                fetchServerToken();
            } else {
                registerNewServer();
                logger.info(LOG_PREFIX + "Server registered with ID: " + serverId);
            }

            // Auto-enable tenant settings (dedicated servers, teacher access, cross-tenant)
            // This is best-effort: may fail if user is not a tenant admin
            tryEditTenantSettings();

            hostServer();

            // Configure server via tooling API (PascalCase, api-version 2.0)
            tryEditServerInfo();

            saveOfficialSession();

            logger.info(LOG_PREFIX + "Server hosted at " + serverIp);
            logger.info(LOG_PREFIX + "Server is fully configured and broadcasted.");
            logger.info(LOG_PREFIX + "Students can now connect from the server list.");

            if (shutdownRequested) {
                logger.debug(LOG_PREFIX + "Shutdown requested during auth flow, skipping scheduled tasks.");
                return;
            }
            scheduleServerUpdates();
            scheduleTokenRefresh();
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Authentication flow failed: " + e.getMessage(), e);
        }
    }

    private void restoreOrAuthenticate() throws IOException, InterruptedException {
        boolean hasToolingSession = refreshToken != null && !refreshToken.isEmpty();
        boolean hasEduSession = eduRefreshToken != null && !eduRefreshToken.isEmpty();

        if (hasToolingSession && hasEduSession) {
            logger.debug(LOG_PREFIX + "Session restored (serverId=%s).", serverId);
            if (serverId == null || serverId.isEmpty()) {
                logger.warning(LOG_PREFIX + "Session has no serverId. Clearing session and re-authenticating...");
                deleteSession();
            } else {
                ensureValidAccessToken(true);
                ensureValidEduAccessToken(true);
                completeAuthFlow();
                return;
            }
        } else if (hasToolingSession || hasEduSession) {
            logger.warning(LOG_PREFIX + "Partial session (missing " +
                    (hasToolingSession ? "edu client token" : "tooling token") + "). Re-authenticating...");
            deleteSession();
        }

        // No session found, need to authenticate with both app IDs via async device code flows
        doDeviceCodeFlows().thenRun(this::completeAuthFlow).exceptionally(ex -> {
            logger.error(LOG_PREFIX + "Authentication flow failed: " + ex.getMessage());
            return null;
        });
    }

    // ---- Public API ----

    /**
     * Returns the current MESS server token, or null if not yet authenticated.
     */
    public @Nullable String getServerToken() {
        return serverToken;
    }

    /**
     * Returns whether the auth manager is operational — either via MESS registration
     * (server token present) or standalone mode (tenant token pool populated).
     */
    public boolean isActive() {
        return (serverToken != null && !serverToken.isEmpty()) || !tenantTokenPool.isEmpty();
    }

    /**
     * Returns the MESS server ID, or null if not configured.
     */
    public @Nullable String getServerId() {
        return serverId;
    }

    /**
     * Returns the public IP registered with MESS, or null if not yet detected.
     */
    public @Nullable String getServerIp() {
        return serverIp;
    }

    /**
     * Returns the epoch second at which the current server token expires.
     */
    public long getServerTokenExpires() {
        return serverTokenExpires;
    }

    /**
     * Deletes the current session and starts a fresh device code authentication flow.
     * @return true if the reset was initiated, false if education is not configured
     */
    public boolean resetAndReauthenticate() {
        if (geyser == null) {
            // setup() was never called
            return false;
        }
        logger.info(LOG_PREFIX + "Resetting session and starting re-authentication...");
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
        if (tokenRefreshTask != null) {
            tokenRefreshTask.cancel(false);
            tokenRefreshTask = null;
        }
        deleteSession();
        geyser.getScheduledThread().execute(this::runAuthFlow);
        return true;
    }

    /**
     * Cancels scheduled tasks, dehosts the server, and cleans up resources.
     */
    public void shutdown() {
        shutdownRequested = true;
        if (logger == null) {
            return;
        }
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        if (tokenRefreshTask != null) {
            tokenRefreshTask.cancel(false);
        }
        if (standaloneRefreshTask != null) {
            standaloneRefreshTask.cancel(false);
        }

        if (serverToken != null) {
            try {
                dehostServer();
            } catch (Exception e) {
                logger.error(LOG_PREFIX + "Failed to dehost server: " + e.getMessage(), e);
            }
        }

        // Only save session data if education was actually initialized
        if (geyser != null && geyser.config().education().tenancyMode() != EducationTenancyMode.OFF) {
            saveOfficialSession();
        }
    }

    // ---- Device Code Flow (OAuth v2.0 with scope parameter) ----

    /**
     * Runs both device code flows asynchronously: first for the tooling app ID
     * (tooling/* endpoints), then for the edu client app ID (server/* endpoints).
     * Returns a future that completes when both flows finish.
     */
    private CompletableFuture<Void> doDeviceCodeFlows() {
        logger.info(LOG_PREFIX + "Two sign-ins are required: one for server management, one for server registration.");

        // Flow 1: Tooling app ID (for tooling/* endpoints)
        logger.info(LOG_PREFIX + "Step 1/2: Sign in for server management (tooling)...");
        return doDeviceCodeFlow(TOOLING_CLIENT_ID, "tooling authentication").thenCompose(toolingTokens -> {
            this.accessToken = toolingTokens.get("access_token").getAsString();
            this.refreshToken = toolingTokens.has("refresh_token")
                    ? toolingTokens.get("refresh_token").getAsString() : null;
            this.accessTokenExpires = parseTokenExpiry(toolingTokens);

            // Flow 2: Edu client app ID (for server/register, server/fetch_token)
            logger.info(LOG_PREFIX + "Step 2/2: Sign in for server registration...");
            return doDeviceCodeFlow(EDU_CLIENT_ID, "server authentication");
        }).thenAccept(eduTokens -> {
            this.eduAccessToken = eduTokens.get("access_token").getAsString();
            this.eduRefreshToken = eduTokens.has("refresh_token")
                    ? eduTokens.get("refresh_token").getAsString() : null;
            this.eduAccessTokenExpires = parseTokenExpiry(eduTokens);

            logger.info(LOG_PREFIX + "Both authentications successful!");
        });
    }

    /**
     * Runs a single device code flow for the given client ID using v2.0 OAuth (scope parameter).
     * Returns a future that completes with the raw token response JSON.
     */
    private CompletableFuture<JsonObject> doDeviceCodeFlow(String clientId, String label) {
        String deviceCodeBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);
        String pollBodyBase = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8);
        return doDeviceCodeFlowRaw(ENTRA_BASE + "/devicecode", ENTRA_BASE + "/token",
                deviceCodeBody, pollBodyBase, "device_code", label);
    }

    /**
     * Runs a single device code flow using v1.0 OAuth (resource parameter).
     * Used for standalone token acquisition via /geyser edu token.
     * Note: v1.0 token endpoint requires "code" parameter name, not "device_code".
     */
    private CompletableFuture<JsonObject> doDeviceCodeFlowV1(String label) {
        String deviceCodeBody = "client_id=" + URLEncoder.encode(EDU_CLIENT_ID, StandardCharsets.UTF_8)
                + "&resource=" + URLEncoder.encode(MESS_RESOURCE, StandardCharsets.UTF_8);
        String pollBodyBase = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(EDU_CLIENT_ID, StandardCharsets.UTF_8)
                + "&resource=" + URLEncoder.encode(MESS_RESOURCE, StandardCharsets.UTF_8);
        return doDeviceCodeFlowRaw(ENTRA_V1_BASE + "/devicecode", ENTRA_V1_BASE + "/token",
                deviceCodeBody, pollBodyBase, "code", label);
    }

    /**
     * Core device code flow implementation. Shared between v1.0 and v2.0 OAuth.
     * Uses self-rescheduling single-shot tasks instead of Thread.sleep
     * to avoid blocking the executor during the polling window.
     */
    private CompletableFuture<JsonObject> doDeviceCodeFlowRaw(String deviceCodeUrl, String tokenUrl,
                                                               String deviceCodeBody, String pollBodyBase,
                                                               String deviceCodeParamName, String label) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject deviceCodeResponse = postForm(deviceCodeUrl, deviceCodeBody);

            String deviceCode = deviceCodeResponse.get("device_code").getAsString();
            String userCode = deviceCodeResponse.get("user_code").getAsString();
            String verificationUri = deviceCodeResponse.has("verification_uri")
                    ? deviceCodeResponse.get("verification_uri").getAsString()
                    : deviceCodeResponse.get("verification_url").getAsString();
            int expiresIn = deviceCodeResponse.get("expires_in").getAsInt();
            int initialInterval = deviceCodeResponse.get("interval").getAsInt();

            logger.info(LOG_PREFIX + "============================================");
            logger.info(LOG_PREFIX + "  Go to: " + verificationUri);
            logger.info(LOG_PREFIX + "  Enter code: " + userCode);
            logger.info(LOG_PREFIX + "  (" + label + ")");
            logger.info(LOG_PREFIX + "============================================");
            logger.info(LOG_PREFIX + "Waiting for sign-in...");
            logger.info(LOG_PREFIX + "If sign-in fails or is blocked by your organization,");
            logger.info(LOG_PREFIX + "use the EduGeyser Token Tool instead. See the setup guide for details.");

            String pollBody = pollBodyBase
                    + "&" + deviceCodeParamName + "=" + URLEncoder.encode(deviceCode, StandardCharsets.UTF_8);

            long deadline = System.currentTimeMillis() + (expiresIn * 1000L);
            AtomicInteger interval = new AtomicInteger(initialInterval);

            ScheduledExecutorService executor = geyser.getScheduledThread();
            schedulePollTick(executor, future, pollBody, tokenUrl, label, deadline, expiresIn, interval);

        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Schedules a single poll tick, then reschedules itself on success/retry.
     * This avoids blocking the executor thread between polls.
     */
    private void schedulePollTick(ScheduledExecutorService executor, CompletableFuture<JsonObject> future,
                                  String pollBody, String tokenUrl, String label, long deadline, int expiresIn,
                                  AtomicInteger interval) {
        executor.schedule(() -> {
            if (future.isDone()) {
                return;
            }
            if (shutdownRequested) {
                future.completeExceptionally(new IOException("Device code flow interrupted by shutdown"));
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                future.completeExceptionally(new IOException(
                        "Device code flow timed out after " + expiresIn + " seconds"));
                return;
            }

            try {
                JsonObject tokenResponse = postForm(tokenUrl, pollBody);
                if (tokenResponse.has("access_token")) {
                    logger.info(LOG_PREFIX + "Authentication successful (" + label + ")!");
                    future.complete(tokenResponse);
                    return;
                }
            } catch (IOException e) {
                String message = e.getMessage();
                if (message != null && message.contains("authorization_pending")) {
                    // Expected — schedule next tick
                    schedulePollTick(executor, future, pollBody, tokenUrl, label, deadline, expiresIn, interval);
                    return;
                }
                if (message != null && message.contains("slow_down")) {
                    interval.addAndGet(5);
                    schedulePollTick(executor, future, pollBody, tokenUrl, label, deadline, expiresIn, interval);
                    return;
                }
                if (message != null && message.contains("expired_token")) {
                    future.completeExceptionally(new IOException("Device code expired before user completed sign-in"));
                    return;
                }
                logger.debug(LOG_PREFIX + "Unexpected error during polling: %s", message);
                future.completeExceptionally(e);
                return;
            }

            // No access_token yet but no error either — schedule next tick
            schedulePollTick(executor, future, pollBody, tokenUrl, label, deadline, expiresIn, interval);
        }, interval.get(), TimeUnit.SECONDS);
    }

    /**
     * Parse token expiry from OAuth response, handling both v1.0 (expires_on as epoch)
     * and v2.0 (expires_in as seconds from now) formats.
     */
    private long parseTokenExpiry(JsonObject tokenResponse) {
        if (tokenResponse.has("expires_on")) {
            return tokenResponse.get("expires_on").getAsLong();
        }
        if (tokenResponse.has("expires_in")) {
            long expiresIn = tokenResponse.get("expires_in").getAsLong();
            return Instant.now().getEpochSecond() + expiresIn;
        }
        // Fallback: assume 1 hour
        logger.warning(LOG_PREFIX + "Token response missing both expires_on and expires_in. Assuming 1 hour expiry.");
        return Instant.now().getEpochSecond() + 3600;
    }

    // ---- Entra Token Refresh (v2.0) ----

    private boolean refreshAccessToken() {
        if (refreshToken == null) {
            return false;
        }
        try {
            String body = "grant_type=refresh_token"
                    + "&client_id=" + URLEncoder.encode(TOOLING_CLIENT_ID, StandardCharsets.UTF_8)
                    + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);
            JsonObject response = postForm(ENTRA_BASE + "/token", body);

            if (response.has("access_token")) {
                this.accessToken = response.get("access_token").getAsString();
                this.refreshToken = response.has("refresh_token")
                        ? response.get("refresh_token").getAsString() : this.refreshToken;
                this.accessTokenExpires = parseTokenExpiry(response);
                logger.debug(LOG_PREFIX + "Tooling token refreshed, expires %s.", formatExpiry(accessTokenExpires));
                saveOfficialSession();
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Failed to refresh tooling token: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * @return true if the access token is valid after this call
     */
    private boolean ensureValidAccessToken(boolean allowReauth) throws IOException, InterruptedException {
        if (!isAccessTokenExpired()) {
            return true;
        }
        if (refreshAccessToken()) {
            return true;
        }
        if (allowReauth) {
            logger.warning(LOG_PREFIX + "Tooling token refresh failed. Re-authenticating...");
            deleteSession();
            doDeviceCodeFlows().thenRun(this::completeAuthFlow).exceptionally(ex -> {
                logger.error(LOG_PREFIX + "Re-authentication failed: " + ex.getMessage());
                return null;
            });
            // Throw to prevent the caller from continuing synchronously — the async flow will handle it
            throw new InterruptedException("Re-authentication initiated asynchronously");
        } else {
            logger.error(LOG_PREFIX + "Tooling token refresh failed. Cannot re-authenticate from a scheduled task. Use '/geyser edu reset' to manually re-authenticate.");
        }
        return false;
    }

    // ---- Edu Client Token Refresh ----

    private boolean refreshEduAccessToken() {
        if (eduRefreshToken == null) {
            return false;
        }
        try {
            String body = "grant_type=refresh_token"
                    + "&client_id=" + URLEncoder.encode(EDU_CLIENT_ID, StandardCharsets.UTF_8)
                    + "&refresh_token=" + URLEncoder.encode(eduRefreshToken, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);
            JsonObject response = postForm(ENTRA_BASE + "/token", body);

            if (response.has("access_token")) {
                this.eduAccessToken = response.get("access_token").getAsString();
                this.eduRefreshToken = response.has("refresh_token")
                        ? response.get("refresh_token").getAsString() : this.eduRefreshToken;
                this.eduAccessTokenExpires = parseTokenExpiry(response);
                logger.debug(LOG_PREFIX + "Edu client token refreshed, expires %s.", formatExpiry(eduAccessTokenExpires));
                saveOfficialSession();
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Failed to refresh edu client token: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * @return true if the edu access token is valid after this call
     */
    private boolean ensureValidEduAccessToken(boolean allowReauth) throws IOException, InterruptedException {
        if (eduAccessTokenExpires > Instant.now().getEpochSecond() + TOKEN_EXPIRY_BUFFER_SECONDS) {
            return true;
        }
        if (refreshEduAccessToken()) {
            return true;
        }
        if (allowReauth) {
            logger.warning(LOG_PREFIX + "Edu client token refresh failed. Re-authenticating...");
            deleteSession();
            doDeviceCodeFlows().thenRun(this::completeAuthFlow).exceptionally(ex -> {
                logger.error(LOG_PREFIX + "Re-authentication failed: " + ex.getMessage());
                return null;
            });
            throw new InterruptedException("Re-authentication initiated asynchronously");
        } else {
            logger.error(LOG_PREFIX + "Edu client token refresh failed. Cannot re-authenticate from a scheduled task. Use '/geyser edu reset' to manually re-authenticate.");
        }
        return false;
    }

    // ---- Tenant Settings (auto-enable dedicated servers) ----

    private void tryEditTenantSettings() {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("DedicatedServerEnabled", true);
            body.addProperty("TeachersAllowed", true);
            body.addProperty("CrossTenantAllowed", true);
            postJsonWithAuth(MESS_BASE + "/tooling/edit_tenant_settings", accessToken, body.toString());
            logger.info(LOG_PREFIX + "Tenant settings configured: dedicated servers enabled, teacher access enabled, cross-tenant enabled.");
        } catch (IOException e) {
            logger.warning(LOG_PREFIX + "Could not update tenant settings (may require Global Admin): " + e.getMessage());
            logger.warning(LOG_PREFIX + "If this is the first setup, ensure dedicated servers are enabled in the admin portal:");
            logger.warning(LOG_PREFIX + "  https://education.minecraft.net/teachertools/en_US/dedicatedservers/");
        }
    }

    // ---- Server Registration ----

    private void registerNewServer() throws IOException {
        String jwtResponse = postEmptyWithAuth(MESS_BASE + "/server/register", eduAccessToken);
        parseServerTokenJwt(jwtResponse);
    }

    // ---- Edit Server Info (PascalCase, api-version 2.0) ----

    private void tryEditServerInfo() {
        if (serverId == null || serverId.isEmpty()) return;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("ServerId", serverId);
            if (serverName != null && !serverName.isEmpty()) {
                body.addProperty("ServerName", serverName);
            }
            body.addProperty("Enabled", true);
            body.addProperty("IsBroadcasted", true);
            body.addProperty("SharingEnabled", true);
            body.addProperty("CrossTenantAllowed", true);
            postJsonWithAuth(MESS_BASE + "/tooling/edit_server_info", accessToken, body.toString(),
                    Map.of("api-version", "2.0"));
            String nameInfo = (serverName != null && !serverName.isEmpty()) ? ", name='" + serverName + "'" : "";
            logger.info(LOG_PREFIX + "Server configured: enabled, broadcasted, sharing on, cross-tenant on" + nameInfo + ".");
        } catch (IOException e) {
            logger.warning(LOG_PREFIX + "Could not update server info: " + e.getMessage());
            logger.warning(LOG_PREFIX + "You may need to enable the server manually in the admin portal:");
            logger.warning(LOG_PREFIX + "  https://education.minecraft.net/teachertools/en_US/dedicatedservers/");
        }
    }

    // ---- Fetch Server Token ----

    private void fetchServerToken() throws IOException {
        if (serverId == null || serverId.isEmpty()) {
            throw new IOException("Cannot fetch server token: no serverId available. Delete " + OFFICIAL_FILE + " and restart to re-register.");
        }
        String url = MESS_BASE + "/server/fetch_token?serverId="
                + URLEncoder.encode(serverId, StandardCharsets.UTF_8);
        String jwtResponse = getWithAuth(url, eduAccessToken);
        parseServerTokenJwt(jwtResponse);
    }

    // ---- Parse JWT ----

    private void parseServerTokenJwt(String jwtResponse) throws IOException {
        this.serverTokenJwt = jwtResponse.trim();
        String[] parts = serverTokenJwt.split("\\.");
        if (parts.length < 2) {
            throw new IOException("Invalid JWT response (got " + parts.length + " parts, expected 3)");
        }

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();

        if (!payload.has("exp") || !payload.has("payload")) {
            throw new IOException("JWT payload missing required fields. Keys present: " + payload.keySet());
        }
        this.serverTokenExpires = payload.get("exp").getAsLong();
        JsonObject innerPayload = payload.getAsJsonObject("payload");

        // Handle both camelCase and PascalCase field names in JWT payloads
        if (innerPayload.has("serverToken")) {
            this.serverToken = innerPayload.get("serverToken").getAsString();
        } else if (innerPayload.has("ServerToken")) {
            this.serverToken = innerPayload.get("ServerToken").getAsString();
        } else {
            throw new IOException("JWT payload missing serverToken field. Keys present: " + innerPayload.keySet());
        }

        // Extract tenant ID and register in multi-tenancy pool
        String tenantId = extractTenantIdFromServerToken(serverToken);
        if (tenantId != null && !tenantId.isEmpty()) {
            registerServerToken(serverToken, tenantId, "MESS tooling registration");
        } else {
            logger.warning(LOG_PREFIX + "Could not extract tenantId from server token. MESS token will still work via fallback.");
        }

        // Extract serverId if we don't have one yet (from registration)
        if (serverId == null || serverId.isEmpty()) {
            if (innerPayload.has("serverId")) {
                this.serverId = innerPayload.get("serverId").getAsString();
                logger.debug(LOG_PREFIX + "Extracted serverId from JWT: %s", serverId);
            } else if (innerPayload.has("ServerId")) {
                this.serverId = innerPayload.get("ServerId").getAsString();
                logger.debug(LOG_PREFIX + "Extracted ServerId from JWT: %s", serverId);
            }
        }
    }

    // ---- Host / Dehost / Update ----

    private void hostServer() throws IOException {
        JsonObject transportInfo = new JsonObject();
        transportInfo.addProperty("ip", serverIp);
        JsonObject connectionInfo = new JsonObject();
        connectionInfo.addProperty("transportType", 0);
        connectionInfo.add("transportInfo", transportInfo);
        JsonObject body = new JsonObject();
        body.add("connectionInfo", connectionInfo);
        postJsonWithAuth(MESS_BASE + "/server/host", serverToken, body.toString());
    }

    private void dehostServer() throws IOException {
        postEmptyWithAuth(MESS_BASE + "/server/dehost", serverToken);
    }

    private void sendServerUpdate() {
        try {
            int eduPlayerCount = countEducationPlayers();
            String json = "{\"playerCount\":" + eduPlayerCount
                    + ",\"maxPlayers\":" + maxPlayers
                    + ",\"health\":" + MESS_HEALTH_OPTIMAL + "}";
            postJsonWithAuth(MESS_BASE + "/server/update", serverToken, json);
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Server update failed: " + e.getMessage(), e);
        }
    }

    private int countEducationPlayers() {
        int count = 0;
        for (GeyserSession session : geyser.onlineConnections()) {
            if (session.isEducationClient()) {
                count++;
            }
        }
        return count;
    }

    // ---- Scheduling ----

    private void scheduleServerUpdates() {
        ScheduledExecutorService thread = geyser.getScheduledThread();
        updateTask = thread.scheduleAtFixedRate(this::sendServerUpdate, 10, 10, TimeUnit.SECONDS);
    }

    private void scheduleTokenRefresh() {
        ScheduledExecutorService thread = geyser.getScheduledThread();
        tokenRefreshTask = thread.scheduleAtFixedRate(() -> {
            try {
                if (!ensureValidAccessToken(false) || !ensureValidEduAccessToken(false)) {
                    return;
                }
                fetchServerToken();
                saveOfficialSession();
            } catch (Exception e) {
                logger.error(LOG_PREFIX + "Scheduled token refresh error: " + e.getMessage(), e);
            }
        }, 30, 30, TimeUnit.MINUTES);
    }

    // ---- Session Persistence ----

    private void deleteSession() {
        synchronized (sessionFileLock) {
            // Clear session data from edu_official.yml but preserve config fields
            this.refreshToken = null;
            this.accessToken = null;
            this.accessTokenExpires = 0;
            this.eduRefreshToken = null;
            this.eduAccessToken = null;
            this.eduAccessTokenExpires = 0;
            this.serverToken = null;
            this.serverTokenJwt = null;
            this.serverTokenExpires = 0;
            saveOfficialSession();
        }
    }

    private boolean isAccessTokenExpired() {
        return accessTokenExpires <= Instant.now().getEpochSecond() + TOKEN_EXPIRY_BUFFER_SECONDS;
    }

    // ---- IP Detection ----

    private @Nullable String detectPublicIp() {
        String[] services = {
                "https://checkip.amazonaws.com",
                "https://api.ipify.org",
                "https://icanhazip.com"
        };
        for (String service : services) {
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) URI.create(service).toURL().openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setRequestMethod("GET");
                int code = con.getResponseCode();
                if (code == 200) {
                    String ip = readStream(con.getInputStream()).trim();
                    if (!ip.isEmpty() && ip.length() < 46) {
                        return ip;
                    }
                }
            } catch (Exception ignored) {
                // Try next service
            } finally {
                if (con != null) con.disconnect();
            }
        }
        return null;
    }

    // ---- HTTP Helpers ----

    private JsonObject postForm(String urlStr, String formBody) throws IOException {
        HttpURLConnection con = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        try {
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setConnectTimeout(HTTP_TIMEOUT);
            con.setReadTimeout(HTTP_TIMEOUT);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(formBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = con.getResponseCode();
            if (code >= 400) {
                String errorBody = readStream(con.getErrorStream());
                if (errorBody.contains("authorization_pending")) {
                    throw new IOException("authorization_pending");
                }
                throw new IOException("HTTP " + code + ": " + errorBody);
            }

            try (InputStreamReader isr = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(isr).getAsJsonObject();
            }
        } finally {
            con.disconnect();
        }
    }

    private String getWithAuth(String urlStr, String bearerToken) throws IOException {
        HttpURLConnection con = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        try {
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + bearerToken);
            con.setRequestProperty("x-request-id", UUID.randomUUID().toString());
            con.setConnectTimeout(HTTP_TIMEOUT);
            con.setReadTimeout(HTTP_TIMEOUT);

            int code = con.getResponseCode();
            if (code >= 400) {
                String errorBody = readStream(con.getErrorStream());
                throw new IOException("HTTP " + code + ": " + errorBody);
            }

            return readStream(con.getInputStream());
        } finally {
            con.disconnect();
        }
    }

    private void postJsonWithAuth(String urlStr, String bearerToken, String jsonBody) throws IOException {
        postJsonWithAuth(urlStr, bearerToken, jsonBody, Map.of());
    }

    private void postJsonWithAuth(String urlStr, String bearerToken, String jsonBody, Map<String, String> extraHeaders) throws IOException {
        HttpURLConnection con = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        try {
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + bearerToken);
            con.setRequestProperty("x-request-id", UUID.randomUUID().toString());
            for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }
            con.setConnectTimeout(HTTP_TIMEOUT);
            con.setReadTimeout(HTTP_TIMEOUT);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = con.getResponseCode();
            if (code >= 400) {
                String errorBody = readStream(con.getErrorStream());
                throw new IOException("HTTP " + code + ": " + errorBody);
            }
        } finally {
            con.disconnect();
        }
    }

    private String postEmptyWithAuth(String urlStr, String bearerToken) throws IOException {
        HttpURLConnection con = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        try {
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + bearerToken);
            con.setRequestProperty("x-request-id", UUID.randomUUID().toString());
            con.setConnectTimeout(HTTP_TIMEOUT);
            con.setReadTimeout(HTTP_TIMEOUT);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(new byte[0]);
            }

            int code = con.getResponseCode();
            if (code >= 400) {
                String errorBody = readStream(con.getErrorStream());
                throw new IOException("HTTP " + code + ": " + errorBody);
            }

            return readStream(con.getInputStream());
        } finally {
            con.disconnect();
        }
    }

    private String readStream(@Nullable InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static @Nullable String getStringOrNull(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        String value = obj.get(key).getAsString();
        return value.isEmpty() ? null : value;
    }

    // ---- Nonce Verification ----

    /**
     * Verifies an education client's nonce against the MESS joiner queue.
     * On-demand: checks the local cache first, then fetches from MESS if needed.
     *
     * @param joinerNonce the client's EduJoinerToHostNonce from BedrockClientData
     * @return true if the nonce was found and verified
     */
    public boolean verifyNonce(String joinerNonce) {
        if (joinerNonce == null || joinerNonce.isEmpty()) {
            return false;
        }

        // Clean expired entries
        long now = System.currentTimeMillis();
        nonceCache.entrySet().removeIf(e -> now - e.getValue().fetchedAt() > NONCE_EXPIRY_MS);

        // Check cache first
        if (nonceCache.containsKey(joinerNonce)) {
            return true;
        }

        // Fetch from MESS and check again
        fetchJoinerInfo();
        return nonceCache.containsKey(joinerNonce);
    }

    /**
     * Fetches pending joiner nonces from MESS and adds them to the cache.
     * Uses a 3-second timeout to avoid blocking the login flow.
     * Fetching flushes the MESS queue, so all returned nonces are cached.
     */
    private void fetchJoinerInfo() {
        if (serverToken == null || serverToken.isEmpty()) {
            return;
        }
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) URI.create(MESS_BASE + "/server/fetch_joiner_info").toURL().openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + serverToken);
            con.setRequestProperty("x-request-id", UUID.randomUUID().toString());
            con.setConnectTimeout(NONCE_FETCH_TIMEOUT);
            con.setReadTimeout(NONCE_FETCH_TIMEOUT);

            int code = con.getResponseCode();
            if (code >= 400) {
                String errorBody = readStream(con.getErrorStream());
                logger.warning(LOG_PREFIX + "fetch_joiner_info failed: HTTP " + code + ": " + errorBody);
                return;
            }

            String responseBody = readStream(con.getInputStream());
            JsonElement parsed = JsonParser.parseString(responseBody);
            JsonArray joiners;
            if (parsed.isJsonArray()) {
                joiners = parsed.getAsJsonArray();
            } else if (parsed.isJsonObject() && parsed.getAsJsonObject().has("joinerSessions")) {
                joiners = parsed.getAsJsonObject().getAsJsonArray("joinerSessions");
            } else {
                logger.warning(LOG_PREFIX + "Unexpected fetch_joiner_info response format: " + responseBody);
                return;
            }
            long now = System.currentTimeMillis();

            for (JsonElement element : joiners) {
                JsonObject joiner = element.getAsJsonObject();
                String nonce = joiner.has("joinerToHostNonce") ? joiner.get("joinerToHostNonce").getAsString() : null;
                String sessionToken = joiner.has("sessionToken") ? joiner.get("sessionToken").getAsString() : null;
                if (nonce != null && !nonce.isEmpty()) {
                    nonceCache.put(nonce, new NonceEntry(sessionToken, now));
                }
            }

            if (!joiners.isEmpty()) {
                logger.debug(LOG_PREFIX + "Fetched %s joiner nonce(s) from MESS", joiners.size());
            }
        } catch (Exception e) {
            logger.warning(LOG_PREFIX + "fetch_joiner_info error: " + e.getMessage());
        } finally {
            if (con != null) con.disconnect();
        }
    }

    /** Increment the verified join counter. */
    public void recordVerifiedJoin() { verifiedJoins.incrementAndGet(); }

    /** Increment the unverified (config-trust) join counter. */
    public void recordUnverifiedJoin() { unverifiedJoins.incrementAndGet(); }

    /** Increment the rejected join counter. */
    public void recordRejectedJoin() { rejectedJoins.incrementAndGet(); }

    public long getVerifiedJoins() { return verifiedJoins.get(); }
    public long getUnverifiedJoins() { return unverifiedJoins.get(); }
    public long getRejectedJoins() { return rejectedJoins.get(); }

    public String formatExpiry(long epochSeconds) {
        if (epochSeconds <= 0) return "never/unset";
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    }

    // ---- Tenant Token Pool Management ----

    /**
     * Register a server token with an already-known tenant ID.
     *
     * @param serverToken the server token value
     * @param tenantId the tenant ID to associate with this token
     * @param source a description of where this token came from (for logging)
     */
    public void registerServerToken(String serverToken, String tenantId, String source) {
        if (tenantId == null || tenantId.isEmpty()) {
            if (logger != null) {
                logger.warning("[EduTenancy] Cannot register token with null/empty tenant ID (source: " + source + ")");
            }
            return;
        }
        // Replace atomically to prevent a brief empty-list window during concurrent reads.
        tenantTokenPool.put(tenantId, new CopyOnWriteArrayList<>(List.of(serverToken)));
        if (logger != null) {
            logger.debug("[EduTenancy] Registered token for tenant %s (source: %s)", tenantId, source);
        }
    }

    /**
     * Register a server token from config. Extracts the tenant ID from the
     * pipe-separated server token format automatically.
     *
     * @param token the raw token string from config
     * @param source a description of where this token came from (for logging)
     */
    public void registerServerTokenFromConfig(String token, String source) {
        String tenantId = extractTenantIdFromServerToken(token);
        if (tenantId != null) {
            tenantTokenPool.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(token);
            configTrustTenants.add(tenantId);
            if (logger != null) {
                logger.debug("[EduTenancy] Registered token for tenant %s (source: %s)", tenantId, source);
            }
        } else if (logger != null) {
            logger.warning("[EduTenancy] Could not extract tenant ID from token (source: " + source + "). Token will not be usable for routing.");
        }
    }

    /**
     * Load manual server tokens from the tokens: section of edu_standalone.yml
     * and register them in the tenant pool as config-trust tenants.
     * Called during initialization for hybrid and standalone tenancy modes.
     */
    public void loadManualTokens() {
        if (standaloneFilePath == null || !Files.exists(standaloneFilePath)) {
            return;
        }
        try {
            var loader = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                    .path(standaloneFilePath).build();
            var node = loader.load();
            var tokensList = node.node("tokens").getList(String.class);
            if (tokensList == null || tokensList.isEmpty()) {
                if (geyser.config().education().tenancyMode() == EducationTenancyMode.STANDALONE) {
                    logger.warning("[EduTenancy] Standalone mode but no tokens in " + STANDALONE_FILE + ". Use /geyser edu token or paste tokens from the Token Tool.");
                }
                return;
            }

            int loaded = 0;
            for (String token : tokensList) {
                if (token != null && !token.isBlank()) {
                    String trimmed = token.trim();
                    String tenantId = extractTenantIdFromServerToken(trimmed);
                    if (tenantId != null) {
                        tenantTokenPool.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(trimmed);
                        configTrustTenants.add(tenantId);
                        logger.info(String.format("[EduTenancy] Registered token for tenant %s (source: %s)", tenantId, STANDALONE_FILE));
                        loaded++;

                        // Warn if token expires within 3 days
                        Instant expiry = extractTokenExpiry(trimmed);
                        if (expiry == Instant.EPOCH) {
                            logger.warning(String.format("[EduTenancy] Could not parse expiry timestamp for tenant %s token.", tenantId));
                        } else {
                            long daysLeft = Duration.between(Instant.now(), expiry).toDays();
                            if (daysLeft < 0) {
                                logger.warning(String.format("[EduTenancy] Token for tenant %s is EXPIRED. Replace it with a fresh token.", tenantId));
                            } else if (daysLeft < 3) {
                                logger.warning(String.format("[EduTenancy] Token for tenant %s expires in %d day(s). Consider replacing it soon.", tenantId, daysLeft));
                            }
                        }
                    } else {
                        logger.warning("[EduTenancy] Could not extract tenant ID from token in " + STANDALONE_FILE + ". Token will not be usable for routing.");
                    }
                }
            }

            if (loaded > 0) {
                logger.info(String.format("[EduTenancy] Loaded %d manual token(s) from %s", loaded, STANDALONE_FILE));
            }

            // Warn about duplicate tenants
            for (Map.Entry<String, List<String>> entry : tenantTokenPool.entrySet()) {
                if (entry.getValue().size() > 1) {
                    logger.info(String.format("[EduTenancy] Multiple tokens (%d) for tenant %s. The newest will be used for routing.",
                        entry.getValue().size(), entry.getKey()));
                }
            }
        } catch (Exception e) {
            logger.error("[EduTenancy] Failed to load tokens from " + STANDALONE_FILE + ": " + e.getMessage(), e);
        }
    }

    /**
     * Extract tenant ID from a pipe-separated server token.
     * Format: tenantId|serverId|expiry|signature.
     *
     * @param serverToken the pipe-separated server token string
     * @return the tenant ID, or null if malformed
     */
    public @Nullable String extractTenantIdFromServerToken(String serverToken) {
        if (serverToken == null || serverToken.isEmpty()) {
            return null;
        }
        String[] parts = serverToken.split("\\|");
        if (parts.length >= 4) {
            String tenantId = parts[0].trim();
            if (!tenantId.isEmpty()) {
                return tenantId;
            }
        }
        if (logger != null) {
            logger.warning("[EduTenancy] Unexpected server token format (" + parts.length + " pipe segments, expected 4). Cannot extract tenant ID.");
        }
        return null;
    }

    /**
     * Extract the expiry timestamp from the 3rd pipe segment of a server token.
     * Format: tenantId|serverId|2026-03-31T18:19:49.483Z|signature
     *
     * @param serverToken the pipe-separated server token string
     * @return the parsed Instant, or {@link Instant#EPOCH} if unparseable
     */
    private static Instant extractTokenExpiry(String serverToken) {
        if (serverToken == null || serverToken.isEmpty()) {
            return Instant.EPOCH;
        }
        String[] parts = serverToken.split("\\|");
        if (parts.length >= 4) {
            try {
                return Instant.parse(parts[2].trim());
            } catch (Exception e) {
                // Log is not available in static context, but callers should handle EPOCH as "unknown"
            }
        }
        return Instant.EPOCH;
    }

    /**
     * Select the newest token from a list by comparing expiry timestamps.
     *
     * @param tokens the list of tokens for a tenant
     * @return the token with the latest expiry, or null if the list is empty
     */
    private static @Nullable String selectNewestToken(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        if (tokens.size() == 1) {
            return tokens.get(0);
        }
        return tokens.stream()
            .max(Comparator.comparing(EducationAuthManager::extractTokenExpiry))
            .orElse(null);
    }

    /**
     * Extract the tenant ID from an EduTokenChain JWT.
     * The payload contains a "chain" field: "tenantId|signature|expiry|nonce".
     *
     * <p>IMPORTANT: Do NOT use BedrockClientData.getTenantId() -- it is always
     * null for Education Edition clients. This is the only reliable source.</p>
     *
     * @param eduTokenChain the raw EduTokenChain JWT from client data
     * @return the tenant ID, or null if extraction fails
     */
    public @Nullable String extractTenantIdFromEduTokenChain(String eduTokenChain) {
        if (eduTokenChain == null || eduTokenChain.isEmpty()) {
            return null;
        }
        try {
            String[] parts = eduTokenChain.split("\\.");
            if (parts.length < 2) {
                if (logger != null) {
                    logger.warning("[EduTenancy] EduTokenChain is not a valid JWT (parts: " + parts.length + ")");
                }
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(EducationChainVerifier.padBase64(parts[1])), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();

            if (!payload.has("chain")) {
                if (logger != null) {
                    logger.warning("[EduTenancy] EduTokenChain payload has no 'chain' field. Keys: " + payload.keySet());
                }
                return null;
            }

            String chain = payload.get("chain").getAsString();
            String[] chainParts = chain.split("\\|");
            if (chainParts.length < 1 || chainParts[0].isEmpty()) {
                if (logger != null) {
                    logger.warning("[EduTenancy] EduTokenChain 'chain' field is empty or has no tenant ID");
                }
                return null;
            }

            String tenantId = chainParts[0];
            if (logger != null) {
                logger.debug("[EduTenancy] Extracted tenant ID from EduTokenChain: %s", tenantId);
            }
            return tenantId;
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("[EduTenancy] Failed to extract tenant ID from EduTokenChain: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Look up the appropriate server token for an education session.
     * Checks the tenant token pool first (by client tenant ID), then falls
     * back to the MESS-registered token.
     *
     * @param session the education client's session
     * @return the server token, or null if none available
     */
    public @Nullable String getTokenForSession(GeyserSession session) {
        String clientTenantId = session.getEducationTenantId();

        // Try tenant-specific token from pool, selecting the newest by expiry
        if (clientTenantId != null && !clientTenantId.isEmpty()) {
            List<String> poolTokens = tenantTokenPool.get(clientTenantId);
            String newest = selectNewestToken(poolTokens);
            if (newest != null) {
                return newest;
            }
        }

        // Fallback: MESS-registered token
        if (serverToken != null && !serverToken.isEmpty()) {
            return serverToken;
        }

        return null;
    }

    /**
     * Checks if a tenant ID was explicitly configured in the server-tokens config list.
     * Used for hybrid mode to distinguish config-trust tenants (allow without nonce)
     * from the owning MESS-registered tenant (requires nonce verification).
     *
     * @param tenantId the tenant ID to check
     * @return true if this tenant was loaded from config
     */
    public boolean isConfigTrustTenant(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return false;
        }
        return configTrustTenants.contains(tenantId);
    }

    /**
     * Get the number of registered tenant tokens (for diagnostics).
     */
    public int getRegisteredTenantCount() {
        return tenantTokenPool.size();
    }

    /**
     * Returns a snapshot of all tenants in the token pool with their status info.
     * Each entry contains: tenant ID, source (MESS/config/device-code), token status, expiry.
     */
    public List<TenantStatusInfo> getTenantStatusList() {
        List<TenantStatusInfo> result = new ArrayList<>();
        // Collect device-code tenant IDs for source detection
        Set<String> deviceCodeTenants = new HashSet<>();
        for (StandaloneTokenEntry entry : standaloneTokens) {
            deviceCodeTenants.add(entry.tenantId);
        }

        for (Map.Entry<String, List<String>> entry : tenantTokenPool.entrySet()) {
            String tenantId = entry.getKey();
            List<String> tokens = entry.getValue();
            String newestToken = selectNewestToken(tokens);

            // Determine source
            String source;
            if (deviceCodeTenants.contains(tenantId)) {
                source = "device-code";
            } else if (configTrustTenants.contains(tenantId)) {
                source = "config";
            } else {
                source = "MESS";
            }

            // Determine expiry and status
            Instant expiry = newestToken != null ? extractTokenExpiry(newestToken) : Instant.EPOCH;
            String status;
            if (newestToken == null) {
                status = "NO TOKEN";
            } else if (expiry == Instant.EPOCH) {
                status = "UNKNOWN";
            } else {
                long daysLeft = java.time.Duration.between(Instant.now(), expiry).toDays();
                if (daysLeft < 0) {
                    status = "EXPIRED";
                } else if (daysLeft < 3) {
                    status = "EXPIRING";
                } else {
                    status = "VALID";
                }
            }

            result.add(new TenantStatusInfo(tenantId, source, status, expiry, tokens.size()));
        }
        return result;
    }

    /**
     * Snapshot of a tenant's status for the /geyser edu status command.
     */
    public record TenantStatusInfo(String tenantId, String source, String status, Instant expiry, int tokenCount) {}

    // ---- Standalone Device Code Token Acquisition ----

    /**
     * Starts a standalone device code flow to obtain a server token for a new tenant.
     * Any M365 Education account (student or teacher) can complete the sign-in.
     * The resulting token is registered as config-trust and persisted to the standalone tokens file.
     * Can be called multiple times to add tokens for different tenants.
     *
     * @return true if the flow was started, false if one is already in progress
     */
    public boolean startStandaloneTokenFlow() {
        if (geyser == null || logger == null) {
            return false;
        }
        if (standaloneFlowInProgress) {
            logger.warning("[EduToken] A device code flow is already in progress. Wait for it to complete.");
            return false;
        }
        standaloneFlowInProgress = true;

        logger.info("[EduToken] Starting device code flow to obtain a standalone server token...");
        logger.info("[EduToken] Sign in with any M365 Education account from the school you want to add.");

        geyser.getScheduledThread().execute(() -> {
            doDeviceCodeFlowV1("standalone token").thenAccept(tokenResponse -> {
                try {
                    String accessToken = tokenResponse.get("access_token").getAsString();
                    String refreshToken = tokenResponse.has("refresh_token")
                            ? tokenResponse.get("refresh_token").getAsString() : null;

                    // Exchange bearer token for server token via discovery /host
                    String serverToken = exchangeForServerToken(accessToken);
                    if (serverToken == null) {
                        logger.error("[EduToken] Failed to exchange bearer token for server token.");
                        return;
                    }

                    String tenantId = extractTenantIdFromServerToken(serverToken);
                    if (tenantId == null) {
                        logger.error("[EduToken] Could not extract tenant ID from obtained server token.");
                        return;
                    }

                    // Register in pool as config-trust
                    tenantTokenPool.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(serverToken);
                    configTrustTenants.add(tenantId);

                    // Store for persistence and refresh
                    StandaloneTokenEntry entry = new StandaloneTokenEntry(tenantId, refreshToken, serverToken);
                    standaloneTokens.add(entry);
                    saveStandaloneTokens();
                    scheduleStandaloneRefresh();

                    logger.info(String.format("[EduToken] Successfully obtained token for tenant %s", tenantId));
                    logger.info(String.format("[EduToken] Token expires: %s",
                            formatExpiry(extractTokenExpiry(serverToken).getEpochSecond())));
                    logger.info("[EduToken] Token has been registered and will auto-refresh.");

                } catch (Exception e) {
                    logger.error("[EduToken] Error during standalone token acquisition: " + e.getMessage(), e);
                } finally {
                    standaloneFlowInProgress = false;
                }
            }).exceptionally(ex -> {
                standaloneFlowInProgress = false;
                logger.error("[EduToken] Device code flow failed: " + ex.getMessage());
                return null;
            });
        });
        return true;
    }

    /**
     * Exchange a bearer access token for a server token via the discovery /host endpoint.
     *
     * @param bearerToken the OAuth access token
     * @return the server token string, or null on failure
     */
    private @Nullable String exchangeForServerToken(String bearerToken) throws IOException {
        String jsonBody = "{\"build\":12232001,\"locale\":\"en_US\",\"maxPlayers\":40,"
                + "\"networkId\":\"" + UUID.randomUUID() + "\","
                + "\"playerCount\":0,\"protocolVersion\":1,"
                + "\"serverDetails\":\"world\",\"serverName\":\"world\","
                + "\"transportType\":2}";

        HttpURLConnection con = (HttpURLConnection) URI.create(DISCOVERY_BASE + "/host").toURL().openConnection();
        try {
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + bearerToken);
            con.setRequestProperty("api-version", "2.0");
            con.setConnectTimeout(HTTP_TIMEOUT);
            con.setReadTimeout(HTTP_TIMEOUT);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = con.getResponseCode();
            if (code >= 400) {
                String errorBody = readStream(con.getErrorStream());
                logger.error("[EduToken] /host returned HTTP " + code + ": " + errorBody);
                return null;
            }

            try (InputStreamReader isr = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject response = JsonParser.parseReader(isr).getAsJsonObject();
                if (response.has("serverToken")) {
                    return response.get("serverToken").getAsString();
                }
                logger.error("[EduToken] /host response missing serverToken field: " + response);
                return null;
            }
        } finally {
            con.disconnect();
        }
    }

    /**
     * Refresh all standalone tokens. Called periodically by the standalone refresh task.
     */
    private void refreshStandaloneTokens() {
        if (standaloneTokens.isEmpty()) {
            return;
        }

        for (StandaloneTokenEntry entry : standaloneTokens) {
            if (entry.refreshToken == null) {
                continue;
            }
            try {
                // Refresh the OAuth access token using v1.0
                String refreshBody = "grant_type=refresh_token"
                        + "&client_id=" + URLEncoder.encode(EDU_CLIENT_ID, StandardCharsets.UTF_8)
                        + "&refresh_token=" + URLEncoder.encode(entry.refreshToken, StandardCharsets.UTF_8)
                        + "&resource=" + URLEncoder.encode(MESS_RESOURCE, StandardCharsets.UTF_8);
                JsonObject tokenResponse = postForm(ENTRA_V1_BASE + "/token", refreshBody);

                String newAccessToken = tokenResponse.get("access_token").getAsString();
                if (tokenResponse.has("refresh_token")) {
                    entry.refreshToken = tokenResponse.get("refresh_token").getAsString();
                }

                // Exchange for a fresh server token
                String newServerToken = exchangeForServerToken(newAccessToken);
                if (newServerToken != null) {
                    // Replace atomically to prevent empty-list window during concurrent reads
                    tenantTokenPool.put(entry.tenantId, new CopyOnWriteArrayList<>(List.of(newServerToken)));
                    entry.serverToken = newServerToken;
                    logger.debug("[EduToken] Refreshed token for tenant %s", entry.tenantId);
                }
            } catch (Exception e) {
                logger.warning("[EduToken] Failed to refresh token for tenant " + entry.tenantId + ": " + e.getMessage());
                // Clear the broken refresh token so the next cycle doesn't retry with it
                entry.refreshToken = null;
                logger.info("[EduToken] Starting re-authentication for tenant " + entry.tenantId + "...");
                reAuthenticateStandaloneToken(entry);
            }
        }
        saveStandaloneTokens();
    }

    /**
     * Re-authenticate a standalone token entry via a new device code flow.
     * Called automatically when a scheduled token refresh fails (e.g., expired refresh token).
     * Only triggers for tokens that were previously obtained via /geyser edu token,
     * so operators who don't use education support are never prompted.
     */
    private void reAuthenticateStandaloneToken(StandaloneTokenEntry entry) {
        doDeviceCodeFlowV1("token renewal for tenant " + entry.tenantId).thenAccept(tokenResponse -> {
            try {
                String accessToken = tokenResponse.get("access_token").getAsString();
                String newRefreshToken = tokenResponse.has("refresh_token")
                        ? tokenResponse.get("refresh_token").getAsString() : null;

                String newServerToken = exchangeForServerToken(accessToken);
                if (newServerToken == null) {
                    logger.error("[EduToken] Re-auth succeeded but failed to exchange for server token (tenant " + entry.tenantId + ")");
                    return;
                }

                entry.refreshToken = newRefreshToken;
                entry.serverToken = newServerToken;
                tenantTokenPool.put(entry.tenantId, new CopyOnWriteArrayList<>(List.of(newServerToken)));
                saveStandaloneTokens();

                logger.info(String.format("[EduToken] Re-authentication successful for tenant %s", entry.tenantId));
            } catch (Exception e) {
                logger.error("[EduToken] Re-auth error for tenant " + entry.tenantId + ": " + e.getMessage(), e);
            }
        }).exceptionally(ex -> {
            logger.error("[EduToken] Re-auth device code flow failed for tenant " + entry.tenantId + ": " + ex.getMessage());
            return null;
        });
    }

    /**
     * Load device-code tokens from the device-code-tokens section of edu_standalone.yml.
     * Re-registers them in the tenant pool and schedules refresh.
     */
    public void loadDeviceCodeTokens() {
        if (standaloneFilePath == null || !Files.exists(standaloneFilePath)) {
            return;
        }

        try {
            var loader = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                    .path(standaloneFilePath).build();
            var node = loader.load();
            var dcTokens = node.node("device-code-tokens").childrenList();

            for (var child : dcTokens) {
                String tenantId = child.node("tenant-id").getString();
                String refToken = child.node("refresh-token").getString();
                String srvToken = child.node("server-token").getString();

                if (tenantId != null && srvToken != null) {
                    StandaloneTokenEntry entry = new StandaloneTokenEntry(tenantId, refToken, srvToken);
                    standaloneTokens.add(entry);

                    // Register in pool as config-trust
                    tenantTokenPool.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(srvToken);
                    configTrustTenants.add(tenantId);

                    logger.info(String.format("[EduToken] Loaded device-code token for tenant %s", tenantId));
                }
            }
        } catch (Exception e) {
            logger.error("[EduToken] Failed to load device-code tokens from " + STANDALONE_FILE + ": " + e.getMessage(), e);
        }

        // Schedule periodic refresh (every 30 minutes, same as MESS tokens)
        if (!standaloneTokens.isEmpty()) {
            scheduleStandaloneRefresh();

            // Re-authenticate any entries with missing refresh tokens
            for (StandaloneTokenEntry entry : standaloneTokens) {
                if (entry.refreshToken == null) {
                    logger.warning(String.format("[EduToken] Device-code token for tenant %s has no refresh token. Starting re-authentication...", entry.tenantId));
                    reAuthenticateStandaloneToken(entry);
                }
            }
        }
    }

    /**
     * Schedule periodic refresh for standalone tokens.
     */
    private void scheduleStandaloneRefresh() {
        if (geyser == null || standaloneRefreshTask != null) {
            return;
        }
        standaloneRefreshTask = geyser.getScheduledThread().scheduleAtFixedRate(
                this::refreshStandaloneTokens, 30, 30, TimeUnit.MINUTES);
    }

    /**
     * Persist device-code tokens to edu_standalone.yml, preserving manual tokens and comments.
     * Reads the current manual tokens from file, then rewrites the entire file.
     */
    private void saveStandaloneTokens() {
        if (standaloneFilePath == null) {
            return;
        }

        synchronized (sessionFileLock) {
            try {
                // Read current manual tokens from file to preserve user edits
                List<String> manualTokens = new ArrayList<>();
                if (Files.exists(standaloneFilePath)) {
                    var loader = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                            .path(standaloneFilePath).build();
                    var node = loader.load();
                    var tokensList = node.node("tokens").getList(String.class);
                    if (tokensList != null) {
                        manualTokens.addAll(tokensList);
                    }
                }

                // Ensure at least 3 paste slots
                while (manualTokens.size() < 3) {
                    manualTokens.add("");
                }

                Files.writeString(standaloneFilePath, buildStandaloneYaml(
                    manualTokens, new ArrayList<>(standaloneTokens)
                ));
            } catch (Exception e) {
                logger.error("[EduToken] Failed to save device-code tokens to " + STANDALONE_FILE + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Get the number of standalone device-code tokens (for diagnostics).
     */
    public int getStandaloneTokenCount() {
        return standaloneTokens.size();
    }

    /**
     * Holds a standalone token entry obtained via /geyser edu token.
     */
    private static class StandaloneTokenEntry {
        final String tenantId;
        volatile @Nullable String refreshToken;
        volatile String serverToken;

        StandaloneTokenEntry(String tenantId, @Nullable String refreshToken, String serverToken) {
            this.tenantId = tenantId;
            this.refreshToken = refreshToken;
            this.serverToken = serverToken;
        }
    }
}
