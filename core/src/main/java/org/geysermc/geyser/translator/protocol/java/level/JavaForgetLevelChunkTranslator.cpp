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

#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.ChunkUtils"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket"

#include "java.util.ArrayList"
#include "java.util.List"

@Translator(packet = ClientboundForgetLevelChunkPacket.class)
public class JavaForgetLevelChunkTranslator extends PacketTranslator<ClientboundForgetLevelChunkPacket> {

    override public void translate(GeyserSession session, ClientboundForgetLevelChunkPacket packet) {
        session.getChunkCache().removeChunk(packet.getX(), packet.getZ());


        List<Vector3i> removedSkulls = new ArrayList<>();
        for (Vector3i position : session.getSkullCache().getSkulls().keySet()) {
            if ((position.getX() >> 4) == packet.getX() && (position.getZ() >> 4) == packet.getZ()) {
                removedSkulls.add(position);
            }
        }
        removedSkulls.forEach(session.getSkullCache()::removeSkull);

        ChunkUtils.sendEmptyChunk(session, packet.getX(), packet.getZ(), false);
    }
}
