/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.ping;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoHandler;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import org.geysermc.common.ping.GeyserPingInfo;
import org.geysermc.connector.GeyserConnector;

import java.util.concurrent.TimeUnit;

public class GeyserLegacyPingPassthrough implements IGeyserPingPassthrough, Runnable {

    private GeyserConnector connector;

    public GeyserLegacyPingPassthrough(GeyserConnector connector) {
        this.connector = connector;
        this.pingInfo = new GeyserPingInfo(null, 0, 0);
    }

    private GeyserPingInfo pingInfo;

    private Client client;

    /**
     * Start legacy ping passthrough thread
     * @param connector GeyserConnector
     * @return GeyserPingPassthrough, or null if not initialized
     */
    public static IGeyserPingPassthrough init(GeyserConnector connector) {
        if (connector.getConfig().isPassthroughMotd() || connector.getConfig().isPassthroughPlayerCounts()) {
            GeyserLegacyPingPassthrough pingPassthrough = new GeyserLegacyPingPassthrough(connector);
            // Ensure delay is not zero
            int interval = (connector.getConfig().getPingPassthroughInterval() == 0) ? 1 : connector.getConfig().getPingPassthroughInterval();
            connector.getLogger().debug("Scheduling ping passthrough at an interval of " + interval + " second(s).");
            connector.getGeneralThreadPool().scheduleAtFixedRate(pingPassthrough, 1, interval, TimeUnit.SECONDS);
            return pingPassthrough;
        }
        return null;
    }

    @Override
    public GeyserPingInfo getPingInformation() {
        return pingInfo;
    }

    @Override
    public void run() {
        try {
            this.client = new Client(connector.getConfig().getRemote().getAddress(), connector.getConfig().getRemote().getPort(), new MinecraftProtocol(SubProtocol.STATUS), new TcpSessionFactory());
            this.client.getSession().setFlag(MinecraftConstants.SERVER_INFO_HANDLER_KEY, (ServerInfoHandler) (session, info) -> {
                this.pingInfo = new GeyserPingInfo(info.getDescription().getFullText(), info.getPlayerInfo().getOnlinePlayers(), info.getPlayerInfo().getMaxPlayers());
                this.client.getSession().disconnect(null);
            });

            client.getSession().connect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
