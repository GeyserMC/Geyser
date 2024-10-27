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

import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;

import java.util.concurrent.TimeUnit;

@Translator(packet = AnimatePacket.class)
public class BedrockAnimateTranslator extends PacketTranslator<AnimatePacket> {

    @Override
    public void translate(GeyserSession session, AnimatePacket packet) {
        // Stop the player sending animations before they have fully spawned into the server
        if (!session.isSpawned()) {
            return;
        }

        if (packet.getAction() == AnimatePacket.Action.SWING_ARM) {
            session.armSwingPending();
            // Delay so entity damage can be processed first
            session.scheduleInEventLoop(() -> {
                    if (session.getArmAnimationTicks() != 0) {
                        // So, generally, a Java player can only do one *thing* at a time.
                        // If a player right-clicks, for example, then there's probably only one action associated with
                        // that right-click that will send a swing.
                        // The only exception I can think of to this, *maybe*, is a player dropping items
                        // Bedrock is a little funkier than this - it can send several arm animation packets in the
                        // same tick, notably with high levels of haste applied.
                        // Packet limiters do not like this and can crash the player.
                        // If arm animation ticks is 0, then we just sent an arm swing packet this tick.
                        // See https://github.com/GeyserMC/Geyser/issues/2875
                        // This behavior was last touched on with ViaVersion 4.5.1 (with its packet limiter), Java 1.16.5,
                        // and Bedrock 1.19.51.
                        // Note for the future: we should probably largely ignore this packet and instead replicate
                        // all actions on our end, and send swings where needed.
                        session.sendDownstreamGamePacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
                        session.activateArmAnimationTicking();
                    }
                },
                25,
                TimeUnit.MILLISECONDS
            );
        }
    }
}
