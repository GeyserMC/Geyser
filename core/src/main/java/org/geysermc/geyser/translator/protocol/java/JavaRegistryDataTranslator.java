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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.TextDecoration;
import org.geysermc.geyser.translator.level.BiomeTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.JavaCodecUtil;

import java.util.Map;

@Translator(packet = ClientboundRegistryDataPacket.class)
public class JavaRegistryDataTranslator extends PacketTranslator<ClientboundRegistryDataPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundRegistryDataPacket packet) {
        Map<String, JavaDimension> dimensions = session.getDimensions();
        dimensions.clear();
        JavaDimension.load(packet.getRegistry(), dimensions);

        Int2ObjectMap<TextDecoration> chatTypes = session.getChatTypes();
        chatTypes.clear();
        for (CompoundTag tag : JavaCodecUtil.iterateAsTag(packet.getRegistry().get("minecraft:chat_type"))) {
            // The ID is NOT ALWAYS THE SAME! ViaVersion as of 1.19 adds two registry entries that do NOT match vanilla.
            int id = ((IntTag) tag.get("id")).getValue();
            CompoundTag element = tag.get("element");
            CompoundTag chat = element.get("chat");
            TextDecoration textDecoration = null;
            if (chat != null) {
                textDecoration = new TextDecoration(chat);
            }
            chatTypes.put(id, textDecoration);
        }

        BiomeTranslator.loadServerBiomes(session, packet.getRegistry());
    }
}
