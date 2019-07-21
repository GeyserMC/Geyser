package org.geysermc.connector.thread;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoHandler;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;

public class PingPassthroughThread implements Runnable {

    private GeyserConnector connector;

    public PingPassthroughThread(GeyserConnector connector) {
        this.connector = connector;
    }

    @Getter
    private ServerStatusInfo info;

    private Client client;

    @Override
    public void run() {
        try {
            this.client = new Client(connector.getConfig().getRemote().getAddress(), connector.getConfig().getRemote().getPort(), new MinecraftProtocol(SubProtocol.STATUS), new TcpSessionFactory());
            this.client.getSession().setFlag(MinecraftConstants.SERVER_INFO_HANDLER_KEY, (ServerInfoHandler) (session, info) -> {
                this.info = info;
                this.client.getSession().disconnect(null);
            });

            client.getSession().connect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
