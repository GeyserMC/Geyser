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

import org.cloudburstmc.protocol.bedrock.packet.SetDifficultyPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;

@Translator(packet = ClientboundChangeDifficultyPacket.class)
public class JavaChangeDifficultyTranslator extends PacketTranslator<ClientboundChangeDifficultyPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundChangeDifficultyPacket packet) {
        Difficulty difficulty = packet.getDifficulty();
        session.getWorldCache().setDifficulty(difficulty);

        // Peaceful difficulty allows always eating food - hence, we just do not send it to Bedrock.
        if (difficulty == Difficulty.PEACEFUL) {
            difficulty = Difficulty.EASY;
        }

        SetDifficultyPacket setDifficultyPacket = new SetDifficultyPacket();
        setDifficultyPacket.setDifficulty(difficulty.ordinal());
        session.sendUpstreamPacket(setDifficultyPacket);

    }
}
