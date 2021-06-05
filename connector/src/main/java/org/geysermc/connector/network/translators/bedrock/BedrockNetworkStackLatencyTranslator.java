/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.nukkitx.protocol.bedrock.packet.NetworkStackLatencyPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.connector.entity.attribute.Attribute;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.AttributeUtils;
import org.geysermc.floodgate.util.DeviceOs;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Used to send the forwarded keep alive packet back to the server
 */
@Translator(packet = NetworkStackLatencyPacket.class)
public class BedrockNetworkStackLatencyTranslator extends PacketTranslator<NetworkStackLatencyPacket> {

    @Override
    public void translate(NetworkStackLatencyPacket packet, GeyserSession session) {
        long pingId;
        // so apparently, as of 1.16.200
        // PS4 divides the network stack latency timestamp FOR US!!!
        // WTF
        if (session.getClientData().getDeviceOs().equals(DeviceOs.PS4)) {
            pingId = packet.getTimestamp();
        } else {
            pingId = packet.getTimestamp() / 1000;
        }

        // negative timestamps are used as hack to fix the url image loading bug
        if (packet.getTimestamp() > 0) {
            if (session.getConnector().getConfig().isForwardPlayerPing()) {
                ClientKeepAlivePacket keepAlivePacket = new ClientKeepAlivePacket(pingId);
                session.sendDownstreamPacket(keepAlivePacket);
            }
            return;
        }

        // Hack to fix the url image loading bug
        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());

        Attribute attribute = session.getPlayerEntity().getAttributes().get(AttributeType.EXPERIENCE_LEVEL);
        if (attribute != null) {
            attributesPacket.setAttributes(Collections.singletonList(AttributeUtils.getBedrockAttribute(attribute)));
        } else {
            attributesPacket.setAttributes(Collections.singletonList(AttributeUtils.getBedrockAttribute(AttributeType.EXPERIENCE_LEVEL.getAttribute(0))));
        }

        session.getConnector().getGeneralThreadPool().schedule(
                () -> session.sendUpstreamPacket(attributesPacket),
                500, TimeUnit.MILLISECONDS);
    }
}
