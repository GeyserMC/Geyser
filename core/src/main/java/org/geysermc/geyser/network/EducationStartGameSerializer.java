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

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v898.serializer.StartGameSerializer_v898;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;

/**
 * Custom StartGamePacket serializer that appends 3 Education Edition string fields
 * after ownerId in level settings. Education Edition's deserializer expects these
 * fields; without them it reads into subsequent packet data and desyncs.
 */
class EducationStartGameSerializer extends StartGameSerializer_v898 {

    static final EducationStartGameSerializer INSTANCE = new EducationStartGameSerializer();

    private EducationStartGameSerializer() {
    }

    @Override
    protected void writeLevelSettings(ByteBuf buffer, BedrockCodecHelper helper, StartGamePacket packet) {
        super.writeLevelSettings(buffer, helper, packet);
        // Education Edition expects 3 extra strings after ownerId
        helper.writeString(buffer, ""); // educationReferrerId
        helper.writeString(buffer, ""); // educationCreatorWorldId
        helper.writeString(buffer, ""); // educationCreatorId
    }

    @Override
    protected void readLevelSettings(ByteBuf buffer, BedrockCodecHelper helper, StartGamePacket packet) {
        super.readLevelSettings(buffer, helper, packet);
        // Read and discard the 3 Education strings if present (e.g. from Education client)
        helper.readString(buffer); // educationReferrerId
        helper.readString(buffer); // educationCreatorWorldId
        helper.readString(buffer); // educationCreatorId
    }
}
