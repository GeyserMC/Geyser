package org.geysermc.plugin;

import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ConnectedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public class GeyserPlugin extends Plugin {

    private List<ConnectedPlayer> players;

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        try {
            Class<? extends ProxyServer> clazz = getProxy().getClass();
            Field field = clazz.getDeclaredField("connections");
            field.setAccessible(true);
            players = (List<ConnectedPlayer>) field.get(getProxy());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        RakNetServer server = new RakNetServer(new InetSocketAddress("0.0.0.0", 65500), 1000000);
        server.addListener(new RakNetServerListener() {
            @Override
            public void handleMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
                if(packet.getId() == 0) {
                }
            }
        });
    }
    private UUID fromXUID(String XUID) {
        String one = XUID.substring(0, XUID.length()/2);
        String two = XUID.substring(XUID.length()/2, XUID.length());
        long L1 = Long.parseLong(one);
        long L2 = Long.parseLong(two);
        return new UUID(L1, L2);
    }
}
