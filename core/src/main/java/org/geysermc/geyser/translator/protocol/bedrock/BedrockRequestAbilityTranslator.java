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

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import com.nukkitx.protocol.bedrock.data.Ability;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.RequestAbilityPacket;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

/**
 * Replaces the AdventureSettingsPacket completely in 1.19.30.
 */
@Translator(packet = RequestAbilityPacket.class)
public class BedrockRequestAbilityTranslator extends PacketTranslator<RequestAbilityPacket> {

    @Override
    public void translate(GeyserSession session, RequestAbilityPacket packet) {
        // Gatekeep to 1.19.30 so older versions don't fire twice
        if (!GameProtocol.supports1_19_30(session)) {
            return;
        }
        
        if (packet.getAbility() == Ability.FLYING) {
            handle(session, packet.isBoolValue());
        }
    }

    //FIXME remove after pre-1.19.30 support is dropped and merge into main method
    static void handle(GeyserSession session, boolean isFlying) {
        if (!isFlying && session.getGameMode() == GameMode.SPECTATOR) {
            // We should always be flying in spectator mode
            session.sendAdventureSettings();
            return;
        } else if (isFlying && session.getPlayerEntity().getFlag(EntityFlag.SWIMMING) && session.getCollisionManager().isPlayerInWater()) {
            // As of 1.18.1, Java Edition cannot fly while in water, but it can fly while crawling
            // If this isn't present, swimming on a 1.13.2 server and then attempting to fly will put you into a flying/swimming state that is invalid on JE
            session.sendAdventureSettings();
            return;
        }

        session.setFlying(isFlying);
        ServerboundPlayerAbilitiesPacket abilitiesPacket = new ServerboundPlayerAbilitiesPacket(isFlying);
        session.sendDownstreamPacket(abilitiesPacket);
    }
}
