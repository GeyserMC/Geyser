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

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.SetSpawnPositionPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.DimensionUtils;

@Translator(packet = ClientboundSetDefaultSpawnPositionPacket.class)
public class JavaSetDefaultSpawnPositionTranslator extends PacketTranslator<ClientboundSetDefaultSpawnPositionPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetDefaultSpawnPositionPacket packet) {
        SetSpawnPositionPacket spawnPositionPacket = new SetSpawnPositionPacket();
        spawnPositionPacket.setBlockPosition(Vector3i.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
        spawnPositionPacket.setSpawnForced(true);
        spawnPositionPacket.setDimensionId(DimensionUtils.javaToBedrock(session.getDimension()));
        spawnPositionPacket.setSpawnType(SetSpawnPositionPacket.Type.WORLD_SPAWN);
        session.sendUpstreamPacket(spawnPositionPacket);
    }
}
