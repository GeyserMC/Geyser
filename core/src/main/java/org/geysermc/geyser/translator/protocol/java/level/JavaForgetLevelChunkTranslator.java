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

package org.geysermc.geyser.translator.protocol.java.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import com.nukkitx.math.vector.Vector3i;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import java.util.Iterator;

@Translator(packet = ClientboundForgetLevelChunkPacket.class)
public class JavaForgetLevelChunkTranslator extends PacketTranslator<ClientboundForgetLevelChunkPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundForgetLevelChunkPacket packet) {
        session.getChunkCache().removeChunk(packet.getX(), packet.getZ());

        //Checks if a skull is in an unloaded chunk then removes it
        Iterator<Vector3i> iterator = session.getSkullCache().keySet().iterator();
        while (iterator.hasNext()) {
            Vector3i position = iterator.next();
            if ((position.getX() >> 4) == packet.getX() && (position.getZ() >> 4) == packet.getZ()) {
                session.getSkullCache().get(position).despawnEntity();
                iterator.remove();
            }
        }

        if (!session.getGeyser().getWorldManager().shouldExpectLecternHandled()) {
            // Do the same thing with lecterns
            iterator = session.getLecternCache().iterator();
            while (iterator.hasNext()) {
                Vector3i position = iterator.next();
                if ((position.getX() >> 4) == packet.getX() && (position.getZ() >> 4) == packet.getZ()) {
                    iterator.remove();
                }
            }
        }
    }
}
