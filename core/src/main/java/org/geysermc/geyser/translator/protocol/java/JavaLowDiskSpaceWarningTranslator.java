/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.packet.ToastRequestPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLowDiskSpaceWarningPacket;

@Translator(packet = ClientboundLowDiskSpaceWarningPacket.class)
public class JavaLowDiskSpaceWarningTranslator extends PacketTranslator<ClientboundLowDiskSpaceWarningPacket> {

    private static final Component LOW_DISK_SPACE = Component.translatable("chunk.toast.lowDiskSpace");
    private static final Component LOW_DISK_SPACE_DESCRIPTION = Component.translatable("chunk.toast.lowDiskSpace.description");

    @Override
    public void translate(GeyserSession session, ClientboundLowDiskSpaceWarningPacket packet) {
        ToastRequestPacket toastRequestPacket = new ToastRequestPacket();
        toastRequestPacket.setTitle(MessageTranslator.convertMessage(LOW_DISK_SPACE, session.locale()));
        toastRequestPacket.setContent(MessageTranslator.convertMessage(LOW_DISK_SPACE_DESCRIPTION, session.locale()));
        session.sendUpstreamPacket(toastRequestPacket);
    }
}
