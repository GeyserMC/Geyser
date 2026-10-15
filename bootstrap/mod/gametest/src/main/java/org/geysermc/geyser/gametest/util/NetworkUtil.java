/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.gametest.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.packet.PacketRegistry;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;

public final class NetworkUtil {

    private NetworkUtil() {}

    public static <MListener extends PacketListener> Packet mojangToGeyserPacket(ProtocolInfo<? extends MListener> protocolInfo, net.minecraft.network.protocol.Packet<?> mojang) {
        ByteBuf buf = Unpooled.buffer();
        protocolInfo.codec().encode(buf, (net.minecraft.network.protocol.Packet<? super MListener>) mojang);

        int mcplId = MinecraftConstants.PACKET_HEADER.readPacketId(buf);
        PacketRegistry packets = MinecraftCodec.CODEC.getCodec(mojangToGeyserProtocolState(protocolInfo.id()));
        return protocolInfo.flow() == PacketFlow.CLIENTBOUND ? packets.createClientboundPacket(mcplId, buf) : packets.createServerboundPacket(mcplId, buf);
    }

    public static ProtocolState mojangToGeyserProtocolState(ConnectionProtocol protocol) {
        return switch (protocol) {
            case HANDSHAKING -> ProtocolState.HANDSHAKE;
            case PLAY -> ProtocolState.GAME;
            case STATUS -> ProtocolState.STATUS;
            case LOGIN -> ProtocolState.LOGIN;
            case CONFIGURATION -> ProtocolState.CONFIGURATION;
        };
    }
}
