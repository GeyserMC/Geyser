/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.edition.mcee.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JWSObject;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserEdition;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Takes care of tokens used to authenticate Client and Server
 */

@Getter
public class TokenManager {
    public static String TOKENS_FILE = "tokens.yml";

    private final GeyserEdition edition;

    private final Map<String, Token> tokenMap = new HashMap<>();

    public TokenManager(GeyserEdition edition) {
        this.edition = edition;

        load();
    }

    /**
     * Load tokens from file
     */
    void load() {
        Yaml yaml = new Yaml();
        Map<String, String> data;

        try (InputStream input = new FileInputStream(new File(TOKENS_FILE))) {
            data = yaml.load(input);
        } catch (IOException e) {
            return;
        }

        for (Map.Entry<String, String> entry : data.entrySet()) {
            tokenMap.put(entry.getKey(), new Token(this, entry.getValue()));
        }
    }

    /**
     * Save tokens to file
     */
    void save() {
        if (tokenMap.size() == 0) {
            return;
        }

        Map<String, String> data = new HashMap<>();

        for (Map.Entry<String, Token> entry : tokenMap.entrySet()) {
            data.put(entry.getKey(), entry.getValue().getRefreshToken());
        }

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(options);

        try (FileWriter output = new FileWriter(new File(TOKENS_FILE))) {
            yaml.dump(data, output);
        } catch (IOException ignored) {
        }
    }

    /**
     * Start OAuth2 Process
     */
    public URL getNewAuthorizationUrl() {
        try {
            return new URL("https://login.microsoftonline.com/common/oauth2/authorize" +
                    "?response_type=" + "code" +
                    "&client_id=" + "b36b1432-1a1c-4c82-9b76-24de1cab42f2" +
                    "&redirect_uri=" + URLEncoder.encode("https://login.microsoftonline.com/common/oauth2/nativeclient", "UTF-8") +
                    "&state=" + UUID.randomUUID().toString() +
                    "&resource=" + URLEncoder.encode("https://meeservices.minecraft.net", "UTF-8"));
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetch Token using authorization response
     */
    public void createInitialToken(String authorizationResponse) throws TokenException {
        if (!authorizationResponse.contains("code=")) {
            throw new TokenException("Invalid authorization response");
        }

        String raw = authorizationResponse.substring(authorizationResponse.indexOf("code="));

        try {
            URL url = new URL("https://login.microsoftonline.com/common/oauth2/token");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoOutput(true);
            connection.setDoInput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            byte[] postData = (raw +
                    "&client_id=" + "b36b1432-1a1c-4c82-9b76-24de1cab42f2" +
                    "&redirect_uri=" + URLEncoder.encode("https://login.microsoftonline.com/common/oauth2/nativeclient", "UTF-8") +
                    "&grant_type=authorization_code").getBytes();
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(postData);
            }

            if (connection.getResponseCode() != 200) {
                throw new TokenException("Failed to create token. Got response: " + connection.getResponseMessage());
            }

            JsonNode node = GeyserConnector.JSON_MAPPER.readTree(connection.getInputStream());

            if (!node.has("refresh_token") || !node.has("access_token")) {
                throw new TokenException("Failed to create token. Missing access/refresh token in response");
            }

            String refreshToken = node.get("refresh_token").asText();
            String accessToken = node.get("access_token").asText();


            JWSObject jwt = JWSObject.parse(accessToken);

            node = GeyserConnector.JSON_MAPPER.readTree(jwt.getPayload().toBytes());
            String tenantId = node.get("tid").asText();

            tokenMap.put(tenantId, new Token(this, refreshToken));
            save();
        } catch (IOException | ParseException e) {
            throw new TokenException("Failed to create token: " + e.getMessage(), e);
        }
    }


    @Getter
    public static class Token {

        // How long till we need to obtain a new signed token, in seconds
        public static int SIGNED_TOKEN_LIFETIME = 604800;
        private final TokenManager manager;
        private String accessToken;
        private String refreshToken;
        private String signedToken;
        private LocalDateTime expires;

        public Token(TokenManager manager, String refreshToken) {
            this.manager = manager;
            this.refreshToken = refreshToken;
        }

        /**
         * Lazily get signed token
         */
        public String getSignedToken() {
            // Refresh token if needed
            if (expires == null || LocalDateTime.now().isAfter(expires)) {
                refresh();
            }

            return signedToken;
        }

        /**
         * Retrieve a new signedToken
         */
        public void refresh() {
            refreshMicrosoftToken();
            refreshMinecraftToken();
            manager.save();
        }

        private void refreshMicrosoftToken() {
            // Refresh Microsoft Token
            try {
                URL url = new URL("https://login.microsoftonline.com/common/oauth2/token");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setDoInput(true);

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                byte[] postData = String.format(
                        "client_id=%s&refresh_token=%s&grant_type=%s",
                        "b36b1432-1a1c-4c82-9b76-24de1cab42f2",
                        refreshToken,
                        "refresh_token"
                ).getBytes();
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    wr.write(postData);
                }

                if (connection.getResponseCode() != 200) {
                    return;
                }

                JsonNode node = GeyserConnector.JSON_MAPPER.readTree(connection.getInputStream());

                accessToken = node.get("access_token").asText();
                refreshToken = node.get("refresh_token").asText();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void refreshMinecraftToken() {
            URL url;
            try {
                url = new URL("https://meeservices.azurewebsites.net/v2/signin");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setDoInput(true);

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");

                ObjectNode postData = GeyserConnector.JSON_MAPPER.createObjectNode();
                postData.put("accessToken", accessToken);
                postData.put("build", 11260000);
                postData.put("clientVersion", 363);
                postData.put("displayVersion", "1.12.60");
                postData.put("identityToken", accessToken);
                postData.put("locale", "en_US");
                postData.put("osVersion", "iOS 13.4.1"); // Be good citizens
                postData.put("platform", "iPad5,3()");  // Be good citizens
                postData.put("requestId", UUID.randomUUID().toString());

                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    wr.write(postData.toString().getBytes());
                }

                if (connection.getResponseCode() != 200) {
                    return;
                }

                JsonNode node = GeyserConnector.JSON_MAPPER.readTree(connection.getInputStream());
                JWSObject jwt = JWSObject.parse(node.get("response").asText());

                node = GeyserConnector.JSON_MAPPER.readTree(jwt.getPayload().toBytes());
                signedToken = node.with("payload").get("signedToken").asText();
                expires = LocalDateTime.now().plus(Duration.ofSeconds(SIGNED_TOKEN_LIFETIME));

            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }

    }

    public static class TokenException extends Exception {
        TokenException(String message) {
            super(message);
        }

        TokenException(String message, Throwable e) {
            super(message, e);
        }
    }


}
