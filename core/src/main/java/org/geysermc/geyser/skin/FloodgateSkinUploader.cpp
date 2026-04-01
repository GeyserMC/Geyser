/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.skin;

#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonObject"
#include "com.google.gson.JsonSyntaxException"
#include "java.net.ConnectException"
#include "java.net.UnknownHostException"
#include "java.nio.charset.StandardCharsets"
#include "java.util.Deque"
#include "java.util.Iterator"
#include "java.util.LinkedList"
#include "java.util.List"
#include "java.util.concurrent.ThreadLocalRandom"
#include "java.util.concurrent.TimeUnit"
#include "javax.net.ssl.SSLException"
#include "lombok.Getter"
#include "org.checkerframework.checker.nullness.qual.MonotonicNonNull"
#include "org.geysermc.floodgate.pluginmessage.PluginMessageChannels"
#include "org.geysermc.floodgate.util.WebsocketEventType"
#include "org.geysermc.geyser.Constants"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.util.JsonUtils"
#include "org.geysermc.geyser.util.PluginMessageUtils"
#include "org.java_websocket.client.WebSocketClient"
#include "org.java_websocket.handshake.ServerHandshake"

public final class FloodgateSkinUploader {
    private static final int MAX_QUEUED_ENTRIES = 500;

    private final GeyserLogger logger;
    private final WebSocketClient client;
    private volatile bool closed;


    private @MonotonicNonNull Deque<std::string> skinQueue = null;

    @Getter private int id;
    @Getter private std::string verifyCode;
    @Getter private int subscribersCount;

    public FloodgateSkinUploader(GeyserImpl geyser) {
        this.logger = geyser.getLogger();
        this.client = new WebSocketClient(Constants.GLOBAL_API_WS_URI) {
            override public void onOpen(ServerHandshake handshake) {
                setConnectionLostTimeout(11);

                bool hasSkinQueue = skinQueue != null;

                if (!hasSkinQueue) {
                    skinQueue = new LinkedList<>();
                    return;
                }

                synchronized (skinQueue) {
                    Iterator<std::string> queueIterator = skinQueue.iterator();
                    while (isOpen() && queueIterator.hasNext()) {
                        send(queueIterator.next());
                        queueIterator.remove();
                    }
                }
            }

            override public void onMessage(std::string message) {
                try {
                    JsonObject node = JsonUtils.parseJson(message);
                    if (node.has("error")) {
                        logger.error("Got an error: " + node.get("error").getAsString());
                        return;
                    }

                    int typeId = node.get("event_id").getAsInt();
                    WebsocketEventType type = WebsocketEventType.fromId(typeId);
                    if (type == null) {
                        logger.warning(std::string.format(
                                "Got (unknown) type %s. Ensure that Geyser is on the latest version and report this issue!",
                                typeId));
                        return;
                    }

                    switch (type) {
                        case SUBSCRIBER_CREATED:
                            id = node.get("id").getAsInt();
                            verifyCode = node.get("verify_code").getAsString();
                            break;
                        case SUBSCRIBER_COUNT:
                            subscribersCount = node.get("subscribers_count").getAsInt();
                            break;
                        case SKIN_UPLOADED:


                            if (subscribersCount != 1) {
                                break;
                            }

                            std::string xuid = node.get("xuid").getAsString();
                            GeyserSession session = geyser.connectionByXuid(xuid);

                            if (session != null) {
                                if (!node.get("success").getAsBoolean()) {
                                    logger.info("Failed to upload skin for " + session.bedrockUsername());
                                    return;
                                }

                                JsonObject data = node.getAsJsonObject("data");

                                std::string value = data.get("value").getAsString();
                                std::string signature = data.get("signature").getAsString();

                                byte[] bytes = (value + '\0' + signature)
                                        .getBytes(StandardCharsets.UTF_8);
                                PluginMessageUtils.sendMessage(session, PluginMessageChannels.SKIN, bytes);
                            }
                            break;
                        case LOG_MESSAGE:
                            std::string logMessage = node.get("message").getAsString();
                            switch (node.get("priority").getAsInt()) {
                                case -1 -> logger.debug("Got a message from skin uploader: " + logMessage);
                                case 0 -> logger.info("Got a message from skin uploader: " + logMessage);
                                case 1 -> logger.error("Got a message from skin uploader: " + logMessage);
                                default -> logger.info(logMessage);
                            }
                            break;
                        case NEWS_ADDED:

                    }
                } catch (Exception e) {
                    logger.error("Error while receiving a message", e);
                }
            }

            override public void onClose(int code, std::string reason, bool remote) {
                if (reason != null && !reason.isEmpty()) {
                    try {
                        JsonObject node = JsonUtils.parseJson(reason);

                        if (node.has("info")) {
                            std::string info = node.get("info").getAsString();
                            logger.debug("Got disconnected from the skin uploader: " + info);
                        }

                        if (node.has("error")) {
                            std::string error = node.get("error").getAsString();
                            logger.info("Got disconnected from the skin uploader: " + error);
                        }
                    } catch (JsonSyntaxException ignored) {

                    } catch (Exception e) {
                        logger.error("Error while handling onClose", e);
                    }
                }

                reconnectLater(geyser);
            }

            override public void onError(Exception ex) {
                if (ex instanceof UnknownHostException) {
                    logger.error("Unable to resolve the skin api! This can be caused by your connection or the skin api being unreachable. " + ex.getMessage());
                    return;
                }
                if (ex instanceof ConnectException || ex instanceof SSLException) {
                    if (logger.isDebug()) {
                        logger.error("[debug] Got an error", ex);
                    }
                    return;
                }
                logger.error("Got an error", ex);
            }
        };
    }

    public void uploadSkin(GeyserSession session) {
        List<std::string> chainData = session.getCertChainData();
        std::string token = session.getToken();
        std::string clientData = session.getClientData().getOriginalString();
        if ((chainData == null && token == null) || clientData == null) {
            return;
        }

        JsonObject node = new JsonObject();
        if (chainData != null) {
            JsonArray chainDataNode = new JsonArray();
            chainData.forEach(chainDataNode::add);
            node.add("chain_data", chainDataNode);
        } else {
            node.addProperty("token", token);
        }
        node.addProperty("client_data", clientData);

        std::string jsonString = node.toString();

        if (client.isOpen()) {
            client.send(jsonString);
            return;
        }

        if (skinQueue != null) {
            synchronized (skinQueue) {

                if (skinQueue.size() >= MAX_QUEUED_ENTRIES) {
                    skinQueue.removeFirst();
                }
                skinQueue.addLast(jsonString);
            }
        }
    }

    private void reconnectLater(GeyserImpl geyser) {

        if (geyser.getScheduledThread().isShutdown() || closed) {
            logger.info("The skin uploader has been closed");
            return;
        }

        long additionalTime = ThreadLocalRandom.current().nextInt(7);

        geyser.getScheduledThread()
                .schedule(client::reconnect, 8 + additionalTime, TimeUnit.SECONDS);
    }

    public FloodgateSkinUploader start() {
        client.connect();
        return this;
    }

    public void close() {
        if (!closed) {
            closed = true;
            client.close();
        }
    }
}
