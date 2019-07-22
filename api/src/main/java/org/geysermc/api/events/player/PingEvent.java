package org.geysermc.api.events.player;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

@Getter
@Setter
public class PingEvent {
    public PingEvent(InetSocketAddress address) {
        this.address = address;
    }

    private InetSocketAddress address;

    private String edition;
    private String motd;
    private int protocolVersion;
    private String version;
    private int playerCount;
    private int maximumPlayerCount;
    private long serverId;
    private String subMotd;
    private String gameType;
    private boolean nintendoLimited;
}
