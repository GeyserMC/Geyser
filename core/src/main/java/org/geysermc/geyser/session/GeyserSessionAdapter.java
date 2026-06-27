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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
                    // both BungeeCord and Velocity remove the IPv6 scope (if there is one) for Spigot
                    int ipv6ScopeIndex = bedrockAddress.indexOf('%');
                    if (ipv6ScopeIndex != -1) {
                        bedrockAddress = bedrockAddress.substring(0, ipv6ScopeIndex);
                    }

                    boolean isEdu = session.isEducationClient();
                    String xuid = session.xuid();
                    // Use the tenant ID extracted from EduTokenChain, NOT clientData.getTenantId() (always null for edu)
                    String tenantId = isEdu && session.getEducationTenantId() != null ? session.getEducationTenantId() : "";
                    int adRole = isEdu ? clientData.getAdRole() : -1;

                    encryptedData = cipher.encryptFromString(BedrockData.of(
                        clientData.getGameVersion(),
                        session.bedrockUsername(),
                        xuid,
                        clientData.getDeviceOs().ordinal(),
                        clientData.getLanguageCode(),
                        clientData.getUiProfile().ordinal(),
                        clientData.getCurrentInputMode().ordinal(),
                        bedrockAddress,
                        skinUploader == null ? 0 : skinUploader.getId(),
                        skinUploader == null ? null : skinUploader.getVerifyCode(),
                        isEdu, tenantId, adRole
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
            // Connected directly to the server
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.connect_internal",
                session.bedrockUsername(), session.getProtocol().getProfile().getName()));
        } else {
            // Connected to an IP address
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.remote.connect",
                session.bedrockUsername(), session.getProtocol().getProfile().getName(), session.remoteServer().address()));
        }

        UUID uuid = session.getProtocol().getProfile().getId();
        if (uuid == null) {
            // Set what our UUID *probably* is going to be
            if (session.remoteServer().authType() == AuthType.FLOODGATE) {
                if (session.isEducationClient()) {
                    // Education clients are guaranteed to have a verified MESS token at this point
                    // (LoginEncryptionUtils rejects them otherwise). The scheme is read explicitly
                    // from the startup-loaded config so this identity decision can't be bypassed.
                    if (geyser.getEducationUuidScheme().legacy()) {
                        uuid = createLegacyEducationUuid(session.getEducationTenantId(), session.bedrockUsername());
                    } else {
                        // The xuid is the MESS-verified Entra OID.
                        uuid = createEducationUuid(session.xuid());
                    }
                } else {
                    uuid = new UUID(0, Long.parseLong(session.xuid()));
                }
            } else {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getProtocol().getProfile().getName()).getBytes(StandardCharsets.UTF_8));
            }
        }
        session.getPlayerEntity().uuid(uuid);
        session.getPlayerEntity().setUsername(session.getProtocol().getProfile().getName());

        String locale = session.getClientData().getLanguageCode();

        // Let the user know there locale may take some time to download
        // as it has to be extracted from a JAR
        if (locale.equalsIgnoreCase("en_us") && !MinecraftLocale.LOCALE_MAPPINGS.containsKey("en_us")) {
            // This should probably be left hardcoded as it will only show for en_us clients
            session.sendMessage("Loading your locale (en_us); if this isn't already downloaded, this may take some time");
        }

        // Download and load the language for the player
        MinecraftLocale.downloadAndLoadLocale(locale);
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        session.loggingIn = false;

        String disconnectMessage, customDisconnectMessage = null;
        Throwable cause = event.getCause();
        if (cause instanceof UnexpectedEncryptionException) {
            if (session.remoteServer().authType() != AuthType.FLOODGATE) {
                // Server expects online mode
                customDisconnectMessage = GeyserLocale.getPlayerLocaleString("geyser.network.remote.authentication_type_mismatch", locale);
                // Explain that they may be looking for Floodgate.
                geyser.getLogger().warning(GeyserLocale.getLocaleStringLog(
                    geyser.platformType() == PlatformType.STANDALONE ?
                        "geyser.network.remote.floodgate_explanation_standalone"
                        : "geyser.network.remote.floodgate_explanation_plugin",
                    Constants.FLOODGATE_DOWNLOAD_LOCATION
                ));
            } else {
                // Likely that Floodgate is not configured correctly.
                customDisconnectMessage = GeyserLocale.getPlayerLocaleString("geyser.network.remote.floodgate_login_error", locale);
                if (geyser.platformType() == PlatformType.STANDALONE) {
                    geyser.getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.network.remote.floodgate_login_error_standalone"));
                }
            }
        } else if (cause instanceof ConnectException) {
            // Server is offline, probably
            customDisconnectMessage = GeyserLocale.getPlayerLocaleString("geyser.network.remote.server_offline", locale);
        }

        // Use our helpful disconnect message whenever possible
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
            // GeyserSession is disconnected via session.disconnect() called indirectly be the server
            // This needs to be "initiated" here when there is an exception, but also when the Netty connection
            // is closed without a disconnect packet - in this case, closed will still be false, but loggedIn
            // will also be true as GeyserSession#disconnect will not have been called.
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

    /**
     * Generate a stable, unique UUID for education players from their Entra OID.
     * The OID is a UUID v4 assigned by Microsoft to an Entra account. It is
     * cryptographically signed in the MESS token, immutable, and globally unique.
     * A person with multiple Entra accounts has multiple OIDs, the same way a
     * person with multiple Xbox accounts has multiple xuids.
     *
     * MSB is fixed so education UUIDs are distinguishable from Bedrock (MSB=0)
     * and Java (random v4). LSB is 64 purely random bits extracted from the OID
     * by stripping the 6 fixed UUID v4 bits (version nibble at bits 48-51,
     * variant at bits 64-65).
     */
    public static final long EDUCATION_UUID_MSB = 0x0000000100000001L;

    static UUID createEducationUuid(String oid) {
        UUID parsed = UUID.fromString(oid);
        long msb = parsed.getMostSignificantBits();
        long lsb = parsed.getLeastSignificantBits();

        // Strip version nibble (bits 48-51) and variant (bits 64-65),
        // pack first 64 purely random bits left-to-right:
        //   bits 0-47 (48 random) + bits 52-63 (12 random) + bits 66-69 (4 random) = 64
        long upper = ((msb >>> 16) << 12) | (msb & 0xFFF);  // 60 random bits
        long lower = (lsb << 2) >>> 60;                       // 4 random bits
        return new UUID(EDUCATION_UUID_MSB, (upper << 4) | lower);
    }

    /**
     * Legacy UUID scheme. Derives an education player's Java UUID from the SHA-256 of
     * {@code tenantId + ":" + username}, taking the first 8 hash bytes as the LSB and the same
     * {@link #EDUCATION_UUID_MSB} sentinel as the MSB. Preserved only for deployments with
     * existing player data keyed by this scheme; selected via {@link EducationUuidScheme}.
     */
    static UUID createLegacyEducationUuid(String tenantId, String username) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((tenantId + ":" + username).getBytes(StandardCharsets.UTF_8));
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xFF);
            }
            return new UUID(EDUCATION_UUID_MSB, lsb);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 not available", e);
        }
    }
}
