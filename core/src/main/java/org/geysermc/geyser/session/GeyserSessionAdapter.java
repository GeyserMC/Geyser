/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session;

import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.network.netty.LocalSession;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.skin.FloodgateSkinUploader;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.UnexpectedEncryptionException;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class GeyserSessionAdapter extends SessionAdapter {

    private final GeyserImpl geyser;
    private final GeyserSession session;
    private final boolean floodgate;
    private final String locale;

    public GeyserSessionAdapter(GeyserSession session) {
        this.session = session;
        this.floodgate = session.remoteServer().authType() == AuthType.FLOODGATE;
        this.geyser = GeyserImpl.getInstance();
        this.locale = session.locale();
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        if (event.getPacket() instanceof ClientIntentionPacket intentionPacket) {
            BedrockClientData clientData = session.getClientData();

            String addressSuffix;
            if (floodgate) {
                byte[] encryptedData;

                try {
                    FloodgateSkinUploader skinUploader = geyser.getSkinUploader();
                    FloodgateCipher cipher = geyser.getCipher();

                    String bedrockAddress = session.getUpstream().getAddress().getAddress().getHostAddress();
                    
                    int ipv6ScopeIndex = bedrockAddress.indexOf('%');
                    if (ipv6ScopeIndex != -1) {
                        bedrockAddress = bedrockAddress.substring(0, ipv6ScopeIndex);
                    }

                    encryptedData = cipher.encryptFromString(BedrockData.of(
                        clientData.getGameVersion(),
                        session.bedrockUsername(),
                        session.xuid(),
                        clientData.getDeviceOs().ordinal(),
                        clientData.getLanguageCode(),
                        clientData.getUiProfile().ordinal(),
                        clientData.getCurrentInputMode().ordinal(),
                        bedrockAddress,
                        skinUploader == null ? 0 : skinUploader.getId(),
                        skinUploader == null ? null : skinUploader.getVerifyCode()
                    ).toString());
                } catch (Exception e) {
                    geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.auth.floodgate.encrypt_fail"), e);
                    session.disconnect(GeyserLocale.getPlayerLocaleString("geyser.auth.floodgate.encrypt_fail", locale));
                    return;
                }

                addressSuffix = '\0' + new String(encryptedData, StandardCharsets.UTF_8);
            } else {
                addressSuffix = "";
            }

            String address;
            if (geyser.config().java().forwardHostname()) {
                address = session.joinAddress();
            } else {
                address = intentionPacket.getHostname();
            }

            event.setPacket(intentionPacket.withHostname(address + addressSuffix));
        }
    }

    @Override
    public void connected(ConnectedEvent event) {
        session.loggingIn = false;
        session.loggedIn = true;

        if (session.getDownstream().getSession() instanceof LocalSession) {
            
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.connect_internal",
                session.bedrockUsername(), session.getProtocol().getProfile().getName()));
        } else {
            
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.connect",
                session.bedrockUsername(), session.getProtocol().getProfile().getName(), session.remoteServer().address()));
        }

        UUID uuid = session.getProtocol().getProfile().getId();
        if (uuid == null) {
            
            if (session.remoteServer().authType() == AuthType.FLOODGATE) {
                uuid = new UUID(0, Long.parseLong(session.xuid()));
            } else {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getProtocol().getProfile().getName()).getBytes(StandardCharsets.UTF_8));
            }
        }
        session.getPlayerEntity().uuid(uuid);
        session.getPlayerEntity().setUsername(session.getProtocol().getProfile().getName());

        String locale = session.getClientData().getLanguageCode();

        
        
        if (locale.equalsIgnoreCase("en_us") && !MinecraftLocale.LOCALE_MAPPINGS.containsKey("en_us")) {
            
            session.sendMessage("Loading your locale (en_us); if this isn't already downloaded, this may take some time");
        }

        
        MinecraftLocale.downloadAndLoadLocale(locale);
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        session.loggingIn = false;

        String disconnectMessage, customDisconnectMessage = null;
        Throwable cause = event.getCause();
        if (cause instanceof UnexpectedEncryptionException) {
            if (session.remoteServer().authType() != AuthType.FLOODGATE) {
                
                customDisconnectMessage = GeyserLocale.getPlayerLocaleString("geyser.network.remote.authentication_type_mismatch", locale);
                
                geyser.getLogger().warning(GeyserLocale.getLocaleStringLog(
                    geyser.platformType() == PlatformType.STANDALONE ?
                        "geyser.network.remote.floodgate_explanation_standalone"
                        : "geyser.network.remote.floodgate_explanation_plugin",
                    Constants.FLOODGATE_DOWNLOAD_LOCATION
                ));
            } else {
                
                customDisconnectMessage = GeyserLocale.getPlayerLocaleString("geyser.network.remote.floodgate_login_error", locale);
                if (geyser.platformType() == PlatformType.STANDALONE) {
                    geyser.getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.network.remote.floodgate_login_error_standalone"));
                }
            }
        } else if (cause instanceof ConnectException) {
            
            customDisconnectMessage = GeyserLocale.getPlayerLocaleString("geyser.network.remote.server_offline", locale);
        }

        
        disconnectMessage = customDisconnectMessage != null ? customDisconnectMessage : MessageTranslator.convertMessage(event.getReason());;

        if (session.getDownstream().getSession() instanceof LocalSession) {
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.disconnect_internal", session.bedrockUsername(), disconnectMessage));
        } else {
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.disconnect", session.bedrockUsername(), session.remoteServer().address(), disconnectMessage));
        }
        if (cause != null) {
            if (cause.getMessage() != null) {
                GeyserImpl.getInstance().getLogger().error(cause.getMessage());
            } else {
                GeyserImpl.getInstance().getLogger().error("An exception occurred: ", cause);
            }
            if (geyser.config().debugMode()) {
                cause.printStackTrace();
            }
        }
        if ((!session.isClosed() && session.loggedIn) || cause != null) {
            
            
            
            
            if (customDisconnectMessage != null) {
                session.disconnect(customDisconnectMessage);
            } else {
                session.disconnect(event.getReason());
            }
        }

        session.loggedIn = false;
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        Registries.JAVA_PACKET_TRANSLATORS.translate(packet.getClass(), packet, this.session, true);
    }

    @Override
    public void packetError(PacketErrorEvent event) {
        geyser.getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.network.downstream_error",
            (event.getPacketClass() != null ? "(" + event.getPacketClass().getSimpleName() + ") " : "") +
                event.getCause().getMessage())
        );
        if (geyser.config().debugMode())
            event.getCause().printStackTrace();
        event.setSuppress(true);
    }
}
