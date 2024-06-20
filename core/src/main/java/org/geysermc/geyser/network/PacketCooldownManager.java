package org.geysermc.geyser.network;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.session.GeyserSession;

import java.util.HashMap;
import java.util.Map;

public class PacketCooldownManager {
    private static final Map<String, CooldownSettings> PACKET_COOLDOWN_SETTINGS = new HashMap<>();

    static {
        setPacketCooldown(LoginPacket.class, -1, 2);
        setPacketCooldown(TextPacket.class, 1000, 10);
        setPacketCooldown(CommandRequestPacket.class, 1000, 10);
        setPacketCooldown(ModalFormResponsePacket.class, 1000, 10);
    }

    private final GeyserSession session;
    @Setter
    private long cooldownMillisDebug;
    private long expiryTimeMillisDebug;

    public static void setPacketCooldown(Class<? extends BedrockPacket> packetClass, int cooldownMillis, int maxCount) {
        PACKET_COOLDOWN_SETTINGS.put(packetClass.getSimpleName(), new CooldownSettings(cooldownMillis, maxCount));
    }

    private final Map<String, CooldownTracker> activeCooldowns = new HashMap<>();

    private boolean isCooldownActive(String packetName) {
        CooldownTracker tracker = activeCooldowns.get(packetName);
        if (tracker != null && tracker.getCount() >= PACKET_COOLDOWN_SETTINGS.get(packetName).maxCount()) {
            if (tracker.getExpiryTime() <= System.currentTimeMillis()) {
                activeCooldowns.remove(packetName);
            } else {
                return true;
            }
        }
        return false;
    }

    private void updateCooldown(String packetName, long cooldownMillis) {
        activeCooldowns.computeIfAbsent(packetName, k -> new CooldownTracker());
        CooldownTracker tracker = activeCooldowns.get(packetName);
        tracker.incrementCount();
        tracker.setExpiryTime(System.currentTimeMillis() + cooldownMillis);
    }

    public boolean handle(BedrockPacket packet) {
        String packetName = packet.getClass().getSimpleName();
        if (PACKET_COOLDOWN_SETTINGS.containsKey(packetName)) {
            CooldownSettings settings = PACKET_COOLDOWN_SETTINGS.get(packetName);
            updateCooldown(packetName, settings.cooldownMillis());
            if (isCooldownActive(packetName)) {
                if (expiryTimeMillisDebug <= System.currentTimeMillis()) {
                    CooldownTracker tracker = activeCooldowns.get(packetName);
                    String message = session.getSocketAddress().getAddress().toString() + " -> Attempted to send too many packets " + packet.getClass().getSimpleName() + "count " + tracker.getCount();
                    if (session.isLoggedIn()) {
                        message += " by user " + session.bedrockUsername();
                    }
                    session.getGeyser().getLogger().debug(message);
                }
                this.expiryTimeMillisDebug = System.currentTimeMillis() + cooldownMillisDebug;
                return false;
            }
        }
        return true;
    }

    public PacketCooldownManager(GeyserSession session, long cooldownMillisDebug) {
        this.session = session;
        this.setCooldownMillisDebug(cooldownMillisDebug);
        this.expiryTimeMillisDebug = 0;
    }

    private record CooldownSettings(int cooldownMillis, int maxCount) {
    }


    @Getter
    private class CooldownTracker {
        private long count;
        @Setter
        private long expiryTime;

        public void incrementCount() {
            this.count++;
        }
    }
}
