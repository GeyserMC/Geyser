/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.command.defaults;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.network.session.GeyserSession;

public class StatisticsCommand extends GeyserCommand {

    private GeyserConnector connector;

    public StatisticsCommand(GeyserConnector connector, String name, String description, String permission) {
        super(name, description, permission);

        this.connector = connector;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.isConsole()) {
            return;
        }

        // Make sure the sender is a Bedrock edition client
        if (sender instanceof GeyserSession) {
            GeyserSession session = (GeyserSession) sender;
            ClientRequestPacket clientRequestPacket = new ClientRequestPacket(ClientRequest.STATS);
            session.sendDownstreamPacket(clientRequestPacket);
            return;
        }
        // Needed for Bukkit - sender is not an instance of GeyserSession
        for (GeyserSession session : connector.getPlayers()) {
            if (sender.getName().equals(session.getPlayerEntity().getUsername())) {
                ClientRequestPacket clientRequestPacket = new ClientRequestPacket(ClientRequest.STATS);
                session.sendDownstreamPacket(clientRequestPacket);
                break;
            }
        }
    }
}
