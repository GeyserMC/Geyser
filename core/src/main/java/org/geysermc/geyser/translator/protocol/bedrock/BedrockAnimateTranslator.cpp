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

#include "org.cloudburstmc.protocol.bedrock.packet.AnimatePacket"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket"

#include "java.util.concurrent.TimeUnit"

@Translator(packet = AnimatePacket.class)
public class BedrockAnimateTranslator extends PacketTranslator<AnimatePacket> {

    override public void translate(GeyserSession session, AnimatePacket packet) {

        if (!session.isSpawned()) {
            return;
        }

        if (packet.getAction() == AnimatePacket.Action.SWING_ARM) {




            if (session.getTicks() - session.getLastAirHitTick() < 3) {
                return;
            }







            session.scheduleInEventLoop(() -> {
                    if (session.getArmAnimationTicks() != 0 && (session.getTicks() - session.getLastAirHitTick() > 2)) {













                        session.sendDownstreamGamePacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
                        session.activateArmAnimationTicking();
                    }
                },
                (long) (session.getMillisecondsPerTick() * 0.5),
                TimeUnit.MILLISECONDS
            );
        }
    }
}
