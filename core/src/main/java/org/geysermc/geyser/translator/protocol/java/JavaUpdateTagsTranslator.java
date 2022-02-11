/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateTagsPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Translator(packet = ClientboundUpdateTagsPacket.class)
public class JavaUpdateTagsTranslator extends PacketTranslator<ClientboundUpdateTagsPacket> {
    private final Map<String, Map<String, int[]>> previous = new HashMap<>();

    @Override
    public void translate(GeyserSession session, ClientboundUpdateTagsPacket packet) {
        for (Map.Entry<String, Map<String, int[]>> entry : packet.getTags().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            StringBuilder builder = new StringBuilder();
            builder.append(entry.getKey()).append("={");
            for (Map.Entry<String, int[]> tag : entry.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                builder.append(tag.getKey()).append('=').append(Arrays.toString(tag.getValue())).append(", ");
            }
            System.out.println(builder.append("}").toString());
        }

        if (previous.isEmpty()) {
            previous.putAll(packet.getTags());
        } else {
            for (Map.Entry<String, Map<String, int[]>> entry : packet.getTags().entrySet()) {
                Map<String, int[]> oldTags = previous.get(entry.getKey());
                for (Map.Entry<String, int[]> newTag : entry.getValue().entrySet()) {
                    int[] oldValue = oldTags.get(newTag.getKey());
                    if (oldValue == null) {
                        System.out.println("Tag " + newTag.getKey() + " not found!!");
                        continue;
                    }
                    if (!Arrays.equals(Arrays.stream(oldValue).sorted().toArray(), Arrays.stream(newTag.getValue()).sorted().toArray())) {
                        System.out.println(entry.getKey() + ": " + newTag.getKey() + " has different values! " + Arrays.toString(Arrays.stream(oldValue).sorted().toArray()) + " " + Arrays.toString(Arrays.stream(newTag.getValue()).sorted().toArray()));
                    }
                }
            }
        }

        session.getTagCache().loadPacket(session, packet);


    }
}
