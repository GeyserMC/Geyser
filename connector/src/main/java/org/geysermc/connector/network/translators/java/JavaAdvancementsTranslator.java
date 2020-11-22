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

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerAdvancementsPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = ServerAdvancementsPacket.class)
public class JavaAdvancementsTranslator extends PacketTranslator<ServerAdvancementsPacket> {

    @Override
    public void translate(ServerAdvancementsPacket packet, GeyserSession session) throws NullPointerException {
        // Removes removed advancements from player's stored advancements
        for (String removedAdvancement : packet.getRemovedAdvancements()) {
            session.getStoredAdvancements().remove(removedAdvancement);
        }

        // Adds advancements to the player's stored advancements when advancements are sent
        for (Advancement advancement : packet.getAdvancements()) {
            if (advancement.getDisplayData() != null && !advancement.getDisplayData().isHidden()){
                session.getStoredAdvancements().put(advancement.getId(), advancement);
            } else {
                session.getStoredAdvancements().remove(advancement.getId());
            }
        }


    }
}
