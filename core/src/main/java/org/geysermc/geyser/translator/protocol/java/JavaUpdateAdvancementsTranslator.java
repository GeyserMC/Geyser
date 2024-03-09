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

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateAdvancementsPacket;
import org.cloudburstmc.protocol.bedrock.packet.ToastRequestPacket;
import org.geysermc.geyser.level.GeyserAdvancement;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.AdvancementsCache;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.Locale;

@Translator(packet = ClientboundUpdateAdvancementsPacket.class)
public class JavaUpdateAdvancementsTranslator extends PacketTranslator<ClientboundUpdateAdvancementsPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundUpdateAdvancementsPacket packet) {
        AdvancementsCache advancementsCache = session.getAdvancementsCache();
        if (packet.isReset()) {
            advancementsCache.getStoredAdvancements().clear();
            advancementsCache.getStoredAdvancementProgress().clear();
        }

        // Removes removed advancements from player's stored advancements
        for (String removedAdvancement : packet.getRemovedAdvancements()) {
            advancementsCache.getStoredAdvancements().remove(removedAdvancement);
        }

        advancementsCache.getStoredAdvancementProgress().putAll(packet.getProgress());

        // Adds advancements to the player's stored advancements when advancements are sent
        for (Advancement advancement : packet.getAdvancements()) {
            if (advancement.getDisplayData() != null && (!advancement.getDisplayData().isHidden() || advancement.getDisplayData().isShowToast())) {
                GeyserAdvancement geyserAdvancement = GeyserAdvancement.from(advancement);
                advancementsCache.getStoredAdvancements().put(advancement.getId(), geyserAdvancement);
            } else {
                advancementsCache.getStoredAdvancements().remove(advancement.getId());
            }
        }

        sendAdvancementToasts(session, packet);
    }

    /**
     * Handle all advancements progress updates
     */
    public void sendAdvancementToasts(GeyserSession session, ClientboundUpdateAdvancementsPacket packet) {
        if (packet.isReset()) {
            // Advancements are being cleared, so they can't be granted
            return;
        }
        for (String advancementId : packet.getProgress().keySet()) {
            GeyserAdvancement advancement = session.getAdvancementsCache().getStoredAdvancements().get(advancementId);
            if (advancement != null && advancement.getDisplayData() != null) {
                if (advancement.getDisplayData().isShowToast() && session.getAdvancementsCache().isEarned(advancement)) {
                    String frameType = advancement.getDisplayData().getAdvancementType().toString().toLowerCase(Locale.ROOT);
                    String frameTitle = advancement.getDisplayColor() + MinecraftLocale.getLocaleString("advancements.toast." + frameType, session.locale());
                    String advancementName = MessageTranslator.convertMessage(advancement.getDisplayData().getTitle(), session.locale());

                    ToastRequestPacket toastRequestPacket = new ToastRequestPacket();
                    toastRequestPacket.setTitle(frameTitle);
                    toastRequestPacket.setContent(advancementName);
                    session.sendUpstreamPacket(toastRequestPacket);
                }
            }
        }
    }
}
