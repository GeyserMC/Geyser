package org.geysermc.plugin;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;
import com.whirvis.jraknet.server.ServerPing;
import net.md_5.bungee.api.connection.ConnectedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeyserPlugin extends Plugin implements Listener {

    private Map<String, String> players = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        System.out.println("abc");
        getProxy().getPluginManager().registerListener(this, this);

        try {
            //TODO: make arguments configurable
            RakNetServer server = new RakNetServer(new InetSocketAddress("localhost", Short.MAX_VALUE), 100);

            server.addListener(new RakNetServerListener() {
                @Override
                public void onConnect(RakNetServer server, InetSocketAddress address, ConnectionType connectionType) {
                    System.out.println("b");
                }

                @Override
                public void onLogin(RakNetServer server, RakNetClientPeer peer) {
                    System.out.println("c");
                }


                @Override
                public void onDisconnect(RakNetServer server, InetSocketAddress address, RakNetClientPeer peer, String reason) {
                    System.out.println(reason);
                }

                @Override
                public void handleMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
                    System.out.println("a");
                    String[] s = packet.readString().split("~~~~");

                    String name = s[0];

                    String uuid = s[1];

                    players.put(name, uuid);

                    peer.sendMessage(Reliability.RELIABLE, new Packet(new byte[]{100, 120, 120}));
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = 127)
    public void onConnect1(LoginEvent e) {
        System.out.println("d");
        if(players.containsKey(e.getConnection().getName())) {
            System.out.println("e");
            e.setCancelled(false);

            e.getConnection().setOnlineMode(false);

            e.getConnection().setUniqueId(fromXUID(players.get(e.getConnection().getName())));
        }
    }

    @EventHandler(priority = 1)
    public void onConnect2(LoginEvent e) {
        System.out.println("f");
        if(players.containsKey(e.getConnection().getName())) {
            System.out.println("g");
            e.setCancelled(false);

            e.getConnection().setOnlineMode(false);

            e.getConnection().setUniqueId(fromXUID(players.get(e.getConnection().getName())));
        }
    }

    private UUID fromXUID(String XUID) {
        String one = XUID.substring(0, XUID.length()/2);
        String two = XUID.substring(XUID.length()/2, XUID.length());
        long L1 = Long.parseLong(one);
        long L2 = Long.parseLong(two);
        return new UUID(L1, L2);
    }
}
