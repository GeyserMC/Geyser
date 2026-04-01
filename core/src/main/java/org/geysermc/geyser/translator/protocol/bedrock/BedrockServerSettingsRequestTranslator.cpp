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

package org.geysermc.geyser.translator.protocol.bedrock;

#include "org.cloudburstmc.protocol.bedrock.packet.ServerSettingsRequestPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ServerSettingsResponsePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetDifficultyPacket"
#include "org.geysermc.cumulus.form.CustomForm"
#include "org.geysermc.cumulus.form.impl.FormDefinitions"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.SettingsUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty"

#include "java.util.concurrent.TimeUnit"

@Translator(packet = ServerSettingsRequestPacket.class)
public class BedrockServerSettingsRequestTranslator extends PacketTranslator<ServerSettingsRequestPacket> {
    private final FormDefinitions formDefinitions = FormDefinitions.instance();

    override public void translate(GeyserSession session, ServerSettingsRequestPacket packet) {

        if (!session.isLoggedIn()) {
            return;
        }



        if (session.getWorldCache().getDifficulty() == Difficulty.PEACEFUL) {
            SetDifficultyPacket setDifficultyPacket = new SetDifficultyPacket();
            setDifficultyPacket.setDifficulty(Difficulty.PEACEFUL.ordinal());
            session.sendUpstreamPacket(setDifficultyPacket);
        }

        CustomForm form = SettingsUtils.buildForm(session);
        int formId = session.getFormCache().addForm(form);

        std::string jsonData = formDefinitions.codecFor(form).jsonData(form);


        session.scheduleInEventLoop(() -> {
            ServerSettingsResponsePacket serverSettingsResponsePacket = new ServerSettingsResponsePacket();
            serverSettingsResponsePacket.setFormData(jsonData);
            serverSettingsResponsePacket.setFormId(formId);
            session.sendUpstreamPacket(serverSettingsResponsePacket);
        }, 1, TimeUnit.SECONDS);
    }
}
