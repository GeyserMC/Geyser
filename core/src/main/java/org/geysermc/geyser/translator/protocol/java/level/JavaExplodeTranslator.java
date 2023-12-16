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

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundExplodePacket;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventGenericPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;

@Translator(packet = ClientboundExplodePacket.class)
public class JavaExplodeTranslator extends PacketTranslator<ClientboundExplodePacket> {

    @Override
    public void translate(GeyserSession session, ClientboundExplodePacket packet) {
        // todo 1.20.3 handle the 4 new fields
        LevelEventGenericPacket levelEventPacket = new LevelEventGenericPacket();
        levelEventPacket.setType(LevelEvent.PARTICLE_BLOCK_EXPLOSION);
        NbtMapBuilder builder = NbtMap.builder();
        builder.putFloat("originX", (float) packet.getX());
        builder.putFloat("originY", (float) packet.getY());
        builder.putFloat("originZ", (float) packet.getZ());
        builder.putFloat("radius", packet.getRadius());
        builder.putInt("size", packet.getExploded().size());
        int i = 0;
        for (Vector3i position : packet.getExploded()) {
            Vector3i pos = Vector3i.from(packet.getX() + position.getX(), packet.getY() + position.getY(), packet.getZ() + position.getZ());
            ChunkUtils.updateBlock(session, BlockStateValues.JAVA_AIR_ID, pos);
            builder.putFloat("pos" + i + "x", pos.getX());
            builder.putFloat("pos" + i + "y", pos.getY());
            builder.putFloat("pos" + i + "z", pos.getZ());
            i++;
        }
        levelEventPacket.setTag(builder.build());
        session.sendUpstreamPacket(levelEventPacket);

        Vector3f pos = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        LevelSoundEventPacket levelSoundEventPacket = new LevelSoundEventPacket();
        levelSoundEventPacket.setRelativeVolumeDisabled(false);
        levelSoundEventPacket.setBabySound(false);
        levelSoundEventPacket.setExtraData(-1);
        levelSoundEventPacket.setSound(SoundEvent.EXPLODE);
        levelSoundEventPacket.setIdentifier(":");
        levelSoundEventPacket.setPosition(pos);
        session.sendUpstreamPacket(levelSoundEventPacket);

        if (packet.getPushX() != 0f || packet.getPushY() != 0f || packet.getPushZ() != 0f) {
            SetEntityMotionPacket motionPacket = new SetEntityMotionPacket();
            motionPacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
            motionPacket.setMotion(Vector3f.from(packet.getPushX(), packet.getPushY(), packet.getPushZ()));
            session.sendUpstreamPacket(motionPacket);
        }
    }
}
