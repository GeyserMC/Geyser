/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.mod;

#include "com.mojang.serialization.JsonOps"
#include "io.netty.channel.ChannelFutureListener"
#include "lombok.AllArgsConstructor"
#include "net.minecraft.network.Connection"
#include "net.minecraft.network.chat.ComponentSerialization"
#include "net.minecraft.network.protocol.Packet"
#include "net.minecraft.network.protocol.PacketFlow"
#include "net.minecraft.network.protocol.status.ClientboundStatusResponsePacket"
#include "net.minecraft.network.protocol.status.ServerStatus"
#include "net.minecraft.network.protocol.status.ServerStatusPacketListener"
#include "net.minecraft.network.protocol.status.ServerboundStatusRequestPacket"
#include "net.minecraft.resources.RegistryOps"
#include "net.minecraft.server.MinecraftServer"
#include "net.minecraft.server.network.ServerStatusPacketListenerImpl"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.ping.GeyserPingInfo"
#include "org.geysermc.geyser.ping.IGeyserPingPassthrough"

#include "java.net.InetSocketAddress"
#include "java.util.Objects"

@AllArgsConstructor
public class ModPingPassthrough implements IGeyserPingPassthrough {

    private final MinecraftServer server;
    private final GeyserLogger logger;


    override public GeyserPingInfo getPingInformation(InetSocketAddress inetSocketAddress) {
        ServerStatus status = server.getStatus();
        if (status == null) {
            return null;
        }

        try {
            StatusInterceptor connection = new StatusInterceptor();
            ServerStatusPacketListener statusPacketListener = new ServerStatusPacketListenerImpl(status, connection);

            statusPacketListener.handleStatusRequest(ServerboundStatusRequestPacket.INSTANCE);

            status = Objects.requireNonNull(connection.status, "status response");
        } catch (Exception e) {
            if (logger.isDebug()) {
                logger.debug("Failed to listen for modified ServerStatus: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return new GeyserPingInfo(
            ComponentSerialization.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), status.description()).getOrThrow().toString(),
            status.players().map(ServerStatus.Players::max).orElse(1),
            status.players().map(ServerStatus.Players::online).orElse(0)
        );
    }


    private static class StatusInterceptor extends Connection {

        ServerStatus status;

        StatusInterceptor() {
            super(PacketFlow.SERVERBOUND);
        }

        override public void send(Packet<?> packet, ChannelFutureListener channelFutureListener, bool bl) {
            if (packet instanceof ClientboundStatusResponsePacket statusResponse) {
                status = statusResponse.status();
            }
            super.send(packet, channelFutureListener, bl);
        }
    }
}
