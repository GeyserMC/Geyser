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

package org.geysermc.connector.network.translators;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateLightPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;

public class PacketTranslatorRegistry<T> {
    private final Map<Class<? extends T>, PacketTranslator<? extends T>> translators = new HashMap<>();

    public static final PacketTranslatorRegistry<Packet> JAVA_TRANSLATOR = new PacketTranslatorRegistry<>();
    public static final PacketTranslatorRegistry<BedrockPacket> BEDROCK_TRANSLATOR = new PacketTranslatorRegistry<>();

    private static final ObjectArrayList<Class<?>> IGNORED_PACKETS = new ObjectArrayList<>();

    static {
        Reflections ref = GeyserConnector.getInstance().useXmlReflections() ? FileUtils.getReflections("org.geysermc.connector.network.translators") : new Reflections("org.geysermc.connector.network.translators");

        for (Class<?> clazz : ref.getTypesAnnotatedWith(Translator.class)) {
            Class<?> packet = clazz.getAnnotation(Translator.class).packet();

            GeyserConnector.getInstance().getLogger().debug("Found annotated translator: " + clazz.getCanonicalName() + " : " + packet.getSimpleName());

            try {
                if (Packet.class.isAssignableFrom(packet)) {
                    Class<? extends Packet> targetPacket = (Class<? extends Packet>) packet;
                    PacketTranslator<? extends Packet> translator = (PacketTranslator<? extends Packet>) clazz.newInstance();

                    JAVA_TRANSLATOR.translators.put(targetPacket, translator);
                } else if (BedrockPacket.class.isAssignableFrom(packet)) {
                    Class<? extends BedrockPacket> targetPacket = (Class<? extends BedrockPacket>) packet;
                    PacketTranslator<? extends BedrockPacket> translator = (PacketTranslator<? extends BedrockPacket>) clazz.newInstance();

                    BEDROCK_TRANSLATOR.translators.put(targetPacket, translator);
                } else {
                    GeyserConnector.getInstance().getLogger().error("Class " + clazz.getCanonicalName() + " is annotated as a translator but has an invalid target packet.");
                }
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error("Could not instantiate annotated translator " + clazz.getCanonicalName());
            }
        }

        IGNORED_PACKETS.add(ServerUpdateLightPacket.class); // Light is handled on Bedrock for us
        IGNORED_PACKETS.add(ServerPlayerListDataPacket.class); // Cant be implemented in bedrock
    }

    private PacketTranslatorRegistry() {
    }

    public static void init() {
        // no-op
    }

    @SuppressWarnings("unchecked")
    public <P extends T> boolean translate(Class<? extends P> clazz, P packet, GeyserSession session) {
        if (!session.getUpstream().isClosed() && !session.isClosed()) {
            try {
                PacketTranslator<P> translator = (PacketTranslator<P>) translators.get(clazz);
                if (translator != null) {
                    translator.translate(packet, session);
                    return true;
                } else {
                    if ((GeyserConnector.getInstance().getPlatformType() != PlatformType.STANDALONE || !(packet instanceof BedrockPacket)) && !IGNORED_PACKETS.contains(clazz)) {
                        // Other debug logs already take care of Bedrock packets for us if on standalone
                        GeyserConnector.getInstance().getLogger().debug("Could not find packet for " + (packet.toString().length() > 25 ? packet.getClass().getSimpleName() : packet));
                    }
                }
            } catch (Throwable ex) {
                GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.network.translator.packet.failed", packet.getClass().getSimpleName()), ex);
                ex.printStackTrace();
            }
        }
        return false;
    }
}
