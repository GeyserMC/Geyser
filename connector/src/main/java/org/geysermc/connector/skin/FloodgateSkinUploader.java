/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.skin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.Constants;
import org.geysermc.connector.utils.PluginMessageUtils;
import org.geysermc.floodgate.util.WebsocketEventType;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.geysermc.connector.utils.PluginMessageUtils.getSkinChannel;

public final class FloodgateSkinUploader {
    private final ObjectMapper JACKSON = new ObjectMapper();
    private final List<String> skinQueue = new ArrayList<>();

    private final GeyserLogger logger;
    private final WebSocketClient client;

    @Getter private int id;
    @Getter private String verifyCode;
    @Getter private int subscribersCount;

    public FloodgateSkinUploader(GeyserConnector connector) {
        this.logger = connector.getLogger();
        this.client = new WebSocketClient(Constants.SKIN_UPLOAD_URI) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                setConnectionLostTimeout(11);

                Iterator<String> queueIterator = skinQueue.iterator();
                while (isOpen() && queueIterator.hasNext()) {
                    send(queueIterator.next());
                    queueIterator.remove();
                }
            }

            @Override
            public void onMessage(String message) {
                // The reason why I don't like Jackson
                try {
                    JsonNode node = JACKSON.readTree(message);
                    if (node.has("error")) {
                        logger.error("Got an error: " + node.get("error").asText());
                        return;
                    }

                    int typeId = node.get("event_id").asInt();
                    WebsocketEventType type = WebsocketEventType.getById(typeId);
                    if (type == null) {
                        logger.warning(String.format(
                                "Got (unknown) type %s. Ensure that Geyser is on the latest version and report this issue!",
                                typeId));
                        return;
                    }

                    switch (type) {
                        case SUBSCRIBER_CREATED:
                            id = node.get("id").asInt();
                            verifyCode = node.get("verify_code").asText();
                            break;
                        case SUBSCRIBERS_COUNT:
                            subscribersCount = node.get("subscribers_count").asInt();
                            break;
                        case SKIN_UPLOADED:
                            // if Geyser is the only subscriber we have send it to the server manually
                            if (subscribersCount != 1) {
                                break;
                            }

                            String xuid = node.get("xuid").asText();
                            String value = node.get("value").asText();
                            String signature = node.get("signature").asText();

                            GeyserSession session = connector.getPlayerByXuid(xuid);
                            if (session != null) {
                                byte[] bytes = (value + '\0' + signature)
                                        .getBytes(StandardCharsets.UTF_8);
                                PluginMessageUtils.sendMessage(session, getSkinChannel(), bytes);
                            }
                            break;
                    }
                } catch (Exception e) {
                    logger.error("Error while receiving a message", e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (reason != null && !reason.isEmpty()) {
                    // The reason why I don't like Jackson
                    try {
                        JsonNode node = JACKSON.readTree(reason);
                        // info means that the uploader itself did nothing wrong
                        if (node.has("info")) {
                            String info = node.get("info").asText();
                            logger.debug("Got disconnected from the skin uploader: " + info);
                        }
                        // error means that the uploader did something wrong
                        if (node.has("error")) {
                            String error = node.get("error").asText();
                            logger.info("Got disconnected from the skin uploader: " + error);
                        }
                        // it can't be something else then info or error, so we won't handle anything other than that.
                        // try to reconnect (which will make a new id and verify token) after a few seconds
                        reconnectLater(connector);
                    } catch (Exception e) {
                        logger.error("Error while handling onClose", e);
                    }
                }
            }

            @Override
            public void onError(Exception ex) {
                logger.error("Got an error", ex);
            }
        };
    }

    public void uploadSkin(JsonNode chainData, String clientData) {
        if (chainData == null || !chainData.isArray() || clientData == null) {
            return;
        }

        ObjectNode node = JACKSON.createObjectNode();
        node.set("chain_data", chainData);
        node.put("client_data", clientData);

        // The reason why I don't like Jackson
        String jsonString;
        try {
            jsonString = JACKSON.writeValueAsString(node);
        } catch (Exception e) {
            logger.error("Failed to upload skin", e);
            return;
        }

        if (client.isOpen()) {
            client.send(jsonString);
            return;
        }
        skinQueue.add(jsonString);
    }

    private void reconnectLater(GeyserConnector connector) {
        //todo doesn't work
        long additionalTime = ThreadLocalRandom.current().nextInt(7);
        connector.getGeneralThreadPool().schedule(() -> {
            try {
                if (!client.connectBlocking()) {
                    reconnectLater(connector);
                }
            } catch (InterruptedException ignored) {
                reconnectLater(connector);
            }
        }, 8 + additionalTime, TimeUnit.SECONDS);
    }

    public FloodgateSkinUploader start() {
        client.connect();
        return this;
    }

    public void stop() {
        client.close();
    }
}
