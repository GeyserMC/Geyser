/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.util.CodeOfConductManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundCodeOfConductPacket;

@Translator(packet = ClientboundCodeOfConductPacket.class)
public class JavaCodeOfConductTranslator extends PacketTranslator<ClientboundCodeOfConductPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundCodeOfConductPacket packet) {
        if (session.hasAcceptedCodeOfConduct()) {
            return;
        } else if (CodeOfConductManager.getInstance().hasAcceptedCodeOfConduct(session, packet.getCodeOfConduct())) {
            session.acceptCodeOfConduct();
            return;
        }
        showCodeOfConductForm(session, packet.getCodeOfConduct());
    }

    private static void showCodeOfConductForm(GeyserSession session, String codeOfConduct) {
        session.prepareForConfigurationForm();
        session.sendForm(CustomForm.builder()
            .translator(MinecraftLocale::getLocaleString, session.locale())
            .title("multiplayer.codeOfConduct.title")
            .label(codeOfConduct)
            .toggle("multiplayer.codeOfConduct.check")
            .validResultHandler(response -> {
                if (response.asToggle()) {
                    CodeOfConductManager.getInstance().saveCodeOfConductAccepted(session, codeOfConduct);
                }
                session.acceptCodeOfConduct();
            })
            .closedResultHandler(() -> session.disconnect(MinecraftLocale.getLocaleString("multiplayer.disconnect.code_of_conduct", session.locale())))
        );
    }
}
