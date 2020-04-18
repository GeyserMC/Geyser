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

package org.geysermc.connector.network.translators;

import java.util.HashMap;
import java.util.Map;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;

import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.protocol.bedrock.BedrockPacket;

public class Registry<T> {
    private final Map<Class<? extends T>, PacketTranslator<? extends T>> MAP = new HashMap<>();

    public static final Registry<Packet> JAVA = new Registry<>();
    public static final Registry<BedrockPacket> BEDROCK = new Registry<>();

    public static void registerJava(Class<? extends Packet> targetPacket, PacketTranslator<? extends Packet> translator) {
        JAVA.MAP.put(targetPacket, translator);
    }

    public static void registerBedrock(Class<? extends BedrockPacket> targetPacket, PacketTranslator<? extends BedrockPacket> translator) {
        BEDROCK.MAP.put(targetPacket, translator);
    }

    @SuppressWarnings("unchecked")
    public <P extends T> boolean translate(Class<? extends P> clazz, P packet, GeyserSession session) {
        if (!session.getUpstream().isClosed() && !session.isClosed()) {
            try {
                if (MAP.containsKey(clazz)) {
                    ((PacketTranslator<P>) MAP.get(clazz)).translate(packet, session);
                    return true;
                } else {
                    GeyserConnector.getInstance().getLogger().debug("Could not find packet for " + (packet.toString().length() > 25 ? packet.getClass().getSimpleName() : packet));
                }
            } catch (Throwable ex) {
                GeyserConnector.getInstance().getLogger().error("Could not translate packet " + packet.getClass().getSimpleName(), ex);
                ex.printStackTrace();
            }
        }
        return false;
    }
}
