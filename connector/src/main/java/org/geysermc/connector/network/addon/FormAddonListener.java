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

package org.geysermc.connector.network.addon;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientPluginMessagePacket;
import com.nukkitx.protocol.bedrock.packet.ModalFormRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ModalFormResponsePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.NetworkUtils;

public class FormAddonListener extends AddonListener<ModalFormResponsePacket> {

    private static final FormAddonListener INSTANCE = new FormAddonListener();

    public static final String NAME = "form";

    public FormAddonListener() {
        super(NAME);
    }

    @Override
    public void onMessageReceive(GeyserSession session, ByteBuf message) {
        ModalFormRequestPacket packet = new ModalFormRequestPacket();
        packet.setFormId(message.readInt());
        packet.setFormData(NetworkUtils.readString(message));
        session.sendUpstreamPacket(packet);
    }

    @Override
    public void handleResponse(GeyserSession session, ModalFormResponsePacket packet) {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        try {
            NetworkUtils.writeString(buffer, this.getSubChannel());
            buffer.writeInt(packet.getFormId());
            NetworkUtils.writeString(buffer, packet.getFormData());

            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            session.sendDownstreamPacket(new ClientPluginMessagePacket(PLUGIN_MESSAGE_CHANNEL, bytes));
        } finally {
            buffer.release();
        }
    }

    public static FormAddonListener get() {
        return INSTANCE;
    }
}
