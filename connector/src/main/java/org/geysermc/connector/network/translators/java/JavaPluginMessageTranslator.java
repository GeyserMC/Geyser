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

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.buffer.ByteBufferNetOutput;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Translator(packet = ServerPluginMessagePacket.class)
public class JavaPluginMessageTranslator extends PacketTranslator<ServerPluginMessagePacket> {
    
    private static final int MAX_VAR_INT_LENGTH = 5;

    @Override
    public void translate(ServerPluginMessagePacket packet, GeyserSession session) {
        if (packet.getChannel().equals("minecraft:brand")) {
            byte[] data;
            try {
                data = writeString(GeyserConnector.NAME);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            session.getDownstream().getSession().send(
                    new ClientPluginMessagePacket(packet.getChannel(), data)
            );
        }
    }

    private byte[] writeString(String string) throws IOException {
        byte[] data = string.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(MAX_VAR_INT_LENGTH + data.length);
        NetOutput output = new ByteBufferNetOutput(byteBuffer);
        output.writeVarInt(data.length);
        output.writeBytes(data);
        return byteBuffer.array();
    }
}
