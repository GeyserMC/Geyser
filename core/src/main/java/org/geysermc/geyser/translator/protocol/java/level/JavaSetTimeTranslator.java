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

package org.geysermc.geyser.translator.protocol.java.level;

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.mcprotocollib.protocol.data.game.level.ClockNetworkState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundSetTimePacket.class)
public class JavaSetTimeTranslator extends PacketTranslator<ClientboundSetTimePacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetTimePacket packet) {
        session.setGameTicks(packet.getGameTime());

        // We only translate the dimension's default clock right now, which will work for vanilla, but probably less so for custom dimensions
        // Nevertheless, this is the best effort we can do right now, and better than just translating the overworld clock
        Key defaultClock = session.getDimensionType().defaultClock();
        if (defaultClock != null) {
            ClockNetworkState currentDimensionState = packet.getClockUpdates().get(JavaRegistries.WORLD_CLOCK.networkId(session, defaultClock));
            if (currentDimensionState != null) {
                session.setTimeTicks(currentDimensionState.totalTicks(), currentDimensionState.partialTick());
                // We need to send a gamerule if this changed
                session.setClockRate(currentDimensionState.rate());
            }
        }
    }
}
