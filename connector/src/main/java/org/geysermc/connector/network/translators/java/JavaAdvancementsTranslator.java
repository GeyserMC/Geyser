/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import com.nukkitx.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.network.session.cache.AdvancementsCache;
import org.geysermc.connector.utils.GeyserAdvancement;
import org.geysermc.connector.utils.LocaleUtils;

import java.util.Map;

@Translator(packet = ServerAdvancementsPacket.class)
public class JavaAdvancementsTranslator extends PacketTranslator<ServerAdvancementsPacket> {

    @Override
    public void translate(GeyserSession session, ServerAdvancementsPacket packet) {
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

        sendToolbarAdvancementUpdates(session, packet);

        // Adds advancements to the player's stored advancements when advancements are sent
        for (Advancement advancement : packet.getAdvancements()) {
            if (advancement.getDisplayData() != null && !advancement.getDisplayData().isHidden()) {
                GeyserAdvancement geyserAdvancement = GeyserAdvancement.from(advancement);
                advancementsCache.getStoredAdvancements().put(advancement.getId(), geyserAdvancement);
            } else {
                advancementsCache.getStoredAdvancements().remove(advancement.getId());
            }
        }
    }

    /**
     * Handle all advancements progress updates
     */
    public void sendToolbarAdvancementUpdates(GeyserSession session, ServerAdvancementsPacket packet) {
        if (packet.isReset()) {
            // Advancements are being cleared, so they can't be granted
            return;
        }
        for (Map.Entry<String, Map<String, Long>> progress : packet.getProgress().entrySet()) {
            GeyserAdvancement advancement = session.getAdvancementsCache().getStoredAdvancements().get(progress.getKey());
            if (advancement != null && advancement.getDisplayData() != null) {
                if (session.getAdvancementsCache().isEarned(advancement)) {
                    // Java uses some pink color for toast challenge completes
                    String color = advancement.getDisplayData().getFrameType() == Advancement.DisplayData.FrameType.CHALLENGE ?
                            "§d" : "§a";
                    String advancementName = MessageTranslator.convertMessage(advancement.getDisplayData().getTitle(), session.getLocale());

                    // Send an action bar message stating they earned an achievement
                    // Sent for instances where broadcasting advancements through chat are disabled
                    SetTitlePacket titlePacket = new SetTitlePacket();
                    titlePacket.setText(color + "[" + LocaleUtils.getLocaleString("advancements.toast." +
                            advancement.getDisplayData().getFrameType().toString().toLowerCase(), session.getLocale()) + "]§f " + advancementName);
                    titlePacket.setType(SetTitlePacket.Type.ACTIONBAR);
                    titlePacket.setFadeOutTime(3);
                    titlePacket.setFadeInTime(3);
                    titlePacket.setStayTime(3);
                    titlePacket.setXuid("");
                    titlePacket.setPlatformOnlineId("");
                    session.sendUpstreamPacket(titlePacket);
                }
            }
        }
    }
}
