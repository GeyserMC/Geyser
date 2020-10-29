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

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.geysermc.common.form.Form;
import org.geysermc.common.form.ModalForm;
import org.geysermc.common.form.util.FormAdaptor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.common.AuthType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Translator(packet = ServerPluginMessagePacket.class)
public class JavaPluginMessageTranslator extends PacketTranslator<ServerPluginMessagePacket> {
    private static final byte[] brandData;

    static {
        byte[] data = GeyserConnector.NAME.getBytes(StandardCharsets.UTF_8);
        byte[] varInt = writeVarInt(data.length);
        brandData = new byte[varInt.length + data.length];
        System.arraycopy(varInt, 0, brandData, 0, varInt.length);
        System.arraycopy(data, 0, brandData, varInt.length, data.length);
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

    @Override
    public void translate(ServerPluginMessagePacket packet, GeyserSession session) {
        String channel = packet.getChannel();

        if (channel.equals("minecraft:brand")) {
            session.sendDownstreamPacket(
                    new ClientPluginMessagePacket(channel, brandData)
            );
        }

        // Floodgate plugin messages
        if (session.getConnector().getAuthType() != AuthType.FLOODGATE) {
            return;
        }

        if (channel.equals("floodgate:form")) {
            byte[] data = packet.getData();

            // receive: first byte is form type, second and third are the id, remaining is the form data
            // respond: first and second byte id, remaining is form response data

            Form.Type type = Form.Type.getByOrdinal(data[0]);
            if (type == null) {
                throw new NullPointerException(
                        "Got type " + data[0] + " which isn't a valid form type!");
            }

            String dataString = new String(data, 3, data.length - 3, Charsets.UTF_8);

            Form form = new GsonBuilder().registerTypeAdapter(ModalForm.class, new FormAdaptor()).create().fromJson(dataString, type.getTypeClass());
            form.setResponseHandler(response -> {
                byte[] raw = response.getBytes(StandardCharsets.UTF_8);
                byte[] finalData = new byte[raw.length + 2];

                finalData[0] = data[1];
                finalData[1] = data[2];
                System.arraycopy(raw, 0, finalData, 2, raw.length);

                session.sendDownstreamPacket(new ClientPluginMessagePacket(channel, finalData));
            });
            session.sendForm(form);
        }
    }
}
