package org.geysermc.plugin;


public class GeyserPlugin {
    /*private List<ConnectedPlayer> players;

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
    }*/
}
