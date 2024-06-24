package org.geysermc.geyser.network;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.session.GeyserSession;

import java.util.HashMap;
import java.util.Map;

public class PacketCooldownManager {
    private final Map<String, CooldownSettings> packetCooldownSettings = new HashMap<>();

    private final GeyserSession session;
    @Setter
    private long cooldownMillisDebug;
    private long expiryTimeMillisDebug;

    public PacketCooldownManager(GeyserSession session, long cooldownMillisDebug) {
        this.session = session;
        this.setCooldownMillisDebug(cooldownMillisDebug);
        this.expiryTimeMillisDebug = 0;

        setPacketCooldown(LoginPacket.class, -1, 2);
        setPacketCooldown(ResourcePackClientResponsePacket.class, -1, 4);
        setPacketCooldown(ResourcePackChunkRequestPacket.class, -1, 0);
        setPacketCooldown(TextPacket.class, 1000, 50);
        setPacketCooldown(CommandRequestPacket.class, 1000, 50);
        setPacketCooldown(ModalFormResponsePacket.class, 1000, 50);
    }

    public void setPacketCooldown(Class<? extends BedrockPacket> packetClass, int cooldownMillis, int maxCount) {
        packetCooldownSettings.put(packetClass.getSimpleName(), new CooldownSettings(cooldownMillis, maxCount));
    }

    private final Map<String, CooldownTracker> activeCooldowns = new HashMap<>();

    private boolean isCooldownActive(BedrockPacket packet) {
        String packetName = packet.getClass().getSimpleName();
        CooldownTracker tracker = activeCooldowns.get(packetName);
        if (tracker != null && tracker.getCount() >= packetCooldownSettings.get(packetName).maxCount()) {
            if (tracker.getExpiryTime() != -1 && tracker.getExpiryTime() <= System.currentTimeMillis()) {
                activeCooldowns.remove(packetName);
            } else {
                return true;
            }
        }
        return false;
    }

    private void updateCooldown(BedrockPacket packet) {
        String packetName = packet.getClass().getSimpleName();
        CooldownSettings settings = packetCooldownSettings.get(packetName);
        CooldownTracker tracker = activeCooldowns.computeIfAbsent(packetName, k -> {
            CooldownTracker newTracker = new CooldownTracker();
            long cooldownMillis = settings.cooldownMillis();
            if (cooldownMillis == -1) {
                newTracker.setExpiryTime(-1);
            } else {
                newTracker.setExpiryTime(System.currentTimeMillis() + cooldownMillis);
            }
            return newTracker;
        });
        tracker.incrementCount();
    }

    public boolean handle(BedrockPacket packet) {
        String packetName = packet.getClass().getSimpleName();
        if (packetCooldownSettings.containsKey(packetName)) {
            updateCooldown(packet);
            if (isCooldownActive(packet)) {
                if (expiryTimeMillisDebug <= System.currentTimeMillis()) {
                    CooldownTracker tracker = activeCooldowns.get(packetName);
                    String message = session.getSocketAddress().getAddress().toString() + " -> Attempted to send too many packets " + packet.getClass().getSimpleName() + " count " + tracker.getCount();
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
