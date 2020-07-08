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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;
import org.reflections.Reflections;

import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.protocol.bedrock.BedrockPacket;

public class PacketTranslatorRegistry<T> {
    private final Map<Class<? extends T>, PacketTranslator<? extends T>> translators = new HashMap<>();

    public static final PacketTranslatorRegistry<Packet> JAVA_TRANSLATOR = new PacketTranslatorRegistry<>();
    public static final PacketTranslatorRegistry<BedrockPacket> BEDROCK_TRANSLATOR = new PacketTranslatorRegistry<>();

    private static final ObjectArrayList<Class<?>> IGNORED_PACKETS = new ObjectArrayList<>();

    public static final Register REGISTER = new Register();

    public static class Register {
        public Register bedrockPacketTranslator(Class<? extends BedrockPacket> packet, PacketTranslator<? extends BedrockPacket> translator) {
            BEDROCK_TRANSLATOR.translators.put(packet, translator);
            return this;
        }

        public Register javaPacketTranslator(Class<? extends Packet> packet, PacketTranslator<? extends Packet> translator) {
            JAVA_TRANSLATOR.translators.put(packet, translator);
            return this;
        }

        public Register ignoredPackets(Class<?> packet) {
            IGNORED_PACKETS.add(packet);
            return this;
        }
    }

    private PacketTranslatorRegistry() {
    }

    @SuppressWarnings("unchecked")
    public <P extends T> boolean translate(Class<? extends P> clazz, P packet, GeyserSession session) {
        if (!session.getUpstream().isClosed() && !session.isClosed()) {
            try {
                if (translators.containsKey(clazz)) {
                    ((PacketTranslator<P>) translators.get(clazz)).translate(packet, session);
                    return true;
                } else {
                    if (!IGNORED_PACKETS.contains(clazz))
                        GeyserConnector.getInstance().getLogger().debug("Could not find packet for " + (packet.toString().length() > 25 ? packet.getClass().getSimpleName() : packet));
                }
            } catch (Throwable ex) {
                GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.network.translator.packet.failed", packet.getClass().getSimpleName()), ex);
                ex.printStackTrace();
            }
        }
        return false;
    }
}
