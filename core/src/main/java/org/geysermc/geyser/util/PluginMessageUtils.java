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

package org.geysermc.geyser.util;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import com.google.common.base.Charsets;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

import java.nio.ByteBuffer;

public class PluginMessageUtils {
    private static final byte[] GEYSER_BRAND_DATA;

    static {
        byte[] data = GeyserImpl.NAME.getBytes(Charsets.UTF_8);
        GEYSER_BRAND_DATA =
                ByteBuffer.allocate(data.length + getVarIntLength(data.length))
                        .put(writeVarInt(data.length))
                        .put(data)
                        .array();
    }

    /**
     * Get the prebuilt brand as a byte array
     *
     * @return the brand information of the Geyser client
     */
    public static byte[] getGeyserBrandData() {
        return GEYSER_BRAND_DATA;
    }

    public static void sendMessage(GeyserSession session, String channel, byte[] data) {
        session.sendDownstreamPacket(new ServerboundCustomPayloadPacket(channel, data));
    }

    private static byte[] writeVarInt(int value) {
        byte[] data = new byte[getVarIntLength(value)];
        int index = 0;
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            data[index] = temp;
            index++;
        } while (value != 0);
        return data;
    }

    private static int getVarIntLength(int number) {
        if ((number & 0xFFFFFF80) == 0) {
            return 1;
        } else if ((number & 0xFFFFC000) == 0) {
            return 2;
        } else if ((number & 0xFFE00000) == 0) {
            return 3;
        } else if ((number & 0xF0000000) == 0) {
            return 4;
        }
        return 5;
    }
}
