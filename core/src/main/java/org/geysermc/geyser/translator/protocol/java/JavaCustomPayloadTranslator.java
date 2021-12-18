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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.google.common.base.Charsets;
import com.nukkitx.protocol.bedrock.packet.EmotePacket;
import com.nukkitx.protocol.bedrock.packet.TransferPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.auth.AuthType;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.Forms;
import org.geysermc.cumulus.util.FormType;
import org.geysermc.geyser.util.PluginMessageUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Translator(packet = ClientboundCustomPayloadPacket.class)
public class JavaCustomPayloadTranslator extends PacketTranslator<ClientboundCustomPayloadPacket> {
    private final GeyserLogger logger = GeyserImpl.getInstance().getLogger();

    @Override
    public void translate(GeyserSession session, ClientboundCustomPayloadPacket  packet) {
        // Handle plugin channels
        switch (packet.getChannel()) {
            case "minecraft:register":
                if (org.geysermc.connector.utils.StringByteUtil.bytesToStrings(packet.getData()).contains(PluginMessageUtils.EMOTE_CHANNEL)) {
                    session.setEmoteChannelOpen(true);
                }
                break;
            case "minecraft:unregister":
                if (org.geysermc.connector.utils.StringByteUtil.bytesToStrings(packet.getData()).contains(PluginMessageUtils.EMOTE_CHANNEL)) {
                    session.setEmoteChannelOpen(false);
                }
                break;
            case "floodgate:form":
                if (session.getRemoteAuthType() == AuthType.FLOODGATE) {
                    handleFloodgateMessage(session, packet);
                }
                break;
            case "geyser:emote":
                handleEmote(session, packet);
                break;
            default:
                //TODO feature: Here we should have a callback for extensions to handle their own plugin messages
        }
    }

    private void handleEmote(GeyserSession session, ClientboundCustomPayloadPacket  packet) {
        EmotePacket emotePacket = new EmotePacket();

        ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData());
        byte[] idBytes = new byte[byteBuffer.get()];
        byteBuffer.get(idBytes);


        emotePacket.setEmoteId(new String(idBytes, StandardCharsets.UTF_8));
        Entity entity = session.getEntityCache().getEntityByJavaId(byteBuffer.getLong());
        if (entity == null) return;
        emotePacket.setRuntimeEntityId(entity.getGeyserId());

        session.sendUpstreamPacket(emotePacket);
    }

    private void handleFloodgateMessage(GeyserSession session, ClientboundCustomPayloadPacket  packet) {
        String channel = packet.getChannel();

        if (channel.equals("floodgate:form")) {
            byte[] data = packet.getData();

            // receive: first byte is form type, second and third are the id, remaining is the form data
            // respond: first and second byte id, remaining is form response data

            FormType type = FormType.getByOrdinal(data[0]);
            if (type == null) {
                throw new NullPointerException(
                        "Got type " + data[0] + " which isn't a valid form type!");
            }

            String dataString = new String(data, 3, data.length - 3, Charsets.UTF_8);

            Form form = Forms.fromJson(dataString, type);
            form.setResponseHandler(response -> {
                byte[] raw = response.getBytes(StandardCharsets.UTF_8);
                byte[] finalData = new byte[raw.length + 2];

                finalData[0] = data[1];
                finalData[1] = data[2];
                System.arraycopy(raw, 0, finalData, 2, raw.length);

                session.sendDownstreamPacket(new ServerboundCustomPayloadPacket(channel, finalData));
            });
            session.sendForm(form);

        } else if (channel.equals("floodgate:transfer")) {
            byte[] data = packet.getData();

            // port, 4 bytes. remaining data, address.

            if (data.length < 5) {
                throw new NullPointerException("Transfer data should be at least 5 bytes long");
            }

            int port = data[0] << 24 | (data[1] & 0xFF) << 16 | (data[2] & 0xFF) << 8 | data[3] & 0xFF;
            String address = new String(data, 4, data.length - 4);

            if (logger.isDebug()) {
                logger.info("Transferring client to: " + address + ":" + port);
            }

            TransferPacket transferPacket = new TransferPacket();
            transferPacket.setAddress(address);
            transferPacket.setPort(port);
            session.sendUpstreamPacket(transferPacket);
        }
    }
}
