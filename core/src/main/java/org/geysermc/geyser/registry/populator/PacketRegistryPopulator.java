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

package org.geysermc.geyser.registry.populator;

import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.FileUtils;

public class PacketRegistryPopulator {

    public static void populate() {
        for (Class<?> clazz : FileUtils.getGeneratedClassesForAnnotation(Translator.class)) {
            Class<?> packet = clazz.getAnnotation(Translator.class).packet();

            GeyserImpl.getInstance().getLogger().debug("Found annotated translator: " + clazz.getCanonicalName() + " : " + packet.getSimpleName());

            try {
                if (Packet.class.isAssignableFrom(packet)) {
                    Class<? extends Packet> targetPacket = (Class<? extends Packet>) packet;
                    PacketTranslator<? extends Packet> translator = (PacketTranslator<? extends Packet>) clazz.newInstance();

                    Registries.JAVA_PACKET_TRANSLATORS.register(targetPacket, translator);
                } else if (BedrockPacket.class.isAssignableFrom(packet)) {
                    Class<? extends BedrockPacket> targetPacket = (Class<? extends BedrockPacket>) packet;
                    PacketTranslator<? extends BedrockPacket> translator = (PacketTranslator<? extends BedrockPacket>) clazz.newInstance();

                    Registries.BEDROCK_PACKET_TRANSLATORS.register(targetPacket, translator);
                } else {
                    GeyserImpl.getInstance().getLogger().error("Class " + clazz.getCanonicalName() + " is annotated as a translator but has an invalid target packet.");
                }
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserImpl.getInstance().getLogger().error("Could not instantiate annotated translator " + clazz.getCanonicalName());
            }
        }
    }
}
