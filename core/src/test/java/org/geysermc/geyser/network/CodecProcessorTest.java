/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network;

import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketSerializer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CodecProcessorTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testCodecs() throws Exception {
        List<BedrockCodec> processedCodecs = GameProtocol.SUPPORTED_BEDROCK_CODECS;

        Assertions.assertFalse(processedCodecs.isEmpty(), "No Bedrock codecs found in GameProtocol!");

        for (BedrockCodec processedCodec : processedCodecs) {
            int version = processedCodec.getProtocolVersion();
            BedrockCodec baseCodec;
            Map<Class<? extends BedrockPacket>, BedrockPacketDefinition<? extends BedrockPacket>> packetDefinitionMap;
            try {
                Class<?> bedrockClass = Class.forName("org.cloudburstmc.protocol.bedrock.codec.v" + version + ".Bedrock_v" + version);
                Field codecField = bedrockClass.getField("CODEC");
                baseCodec = (BedrockCodec) codecField.get(null);
                Field packetDefinitionsField = BedrockCodec.class.getDeclaredField("packetsByClass");
                packetDefinitionsField.setAccessible(true);
                packetDefinitionMap = (Map<Class<? extends BedrockPacket>, BedrockPacketDefinition<? extends BedrockPacket>>) packetDefinitionsField.get(baseCodec);
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                throw new RuntimeException("Could not find original Bedrock codec for version " + version, e);
            }

            for (Class<? extends BedrockPacket> packetClass : packetDefinitionMap.keySet()) {
                checkSerializer(processedCodec, baseCodec, packetClass);
            }
        }
    }

    private <T extends BedrockPacket> void checkSerializer(BedrockCodec processed, BedrockCodec base, Class<T> packetClass) {
        BedrockPacketDefinition<T> processedDefinition = processed.getPacketDefinition(packetClass);
        BedrockPacketDefinition<T> baseDefinition = base.getPacketDefinition(packetClass);

        if (processedDefinition == null || baseDefinition == null) {
            throw new IllegalArgumentException("Unable to find packet definition (custom: %s, base: %s) for packet %s!".formatted(
                processedDefinition, baseDefinition, packetClass.getSimpleName()
            ));
        }

        BedrockPacketSerializer<T> processedSerializer = processedDefinition.getSerializer();
        BedrockPacketSerializer<T> baseSerializer = baseDefinition.getSerializer();

        // No changes on our end, we can skip checking this serializer
        if (Objects.equals(processedSerializer, baseSerializer)) {
            return;
        }

        // Now: check if our custom serializer extends the base one
        Class<?> processedClass = processedSerializer.getClass();
        Class<?> baseClass = baseSerializer.getClass();

        // Illegal / Ignored serializers aren't version-specific and should be ignored
        if (Objects.equals(CodecProcessor.ILLEGAL_SERIALIZER, processedSerializer) || Objects.equals(CodecProcessor.IGNORED_SERIALIZER, processedSerializer)) {
            return;
        }

        Assertions.assertTrue(baseClass.isAssignableFrom(processedClass),
                "Custom serializer " + processedClass.getName() + " for " + packetClass.getSimpleName() +
                        " does not extend the base serializer " + baseClass.getName() + " in protocol " + processed.getProtocolVersion());
        }
}
