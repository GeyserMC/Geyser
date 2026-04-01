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

#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.protocol.bedrock.data.LevelEvent"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventGenericPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.SoundUtils"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundExplodePacket"

#include "java.util.concurrent.ThreadLocalRandom"

@Translator(packet = ClientboundExplodePacket.class)
public class JavaExplodeTranslator extends PacketTranslator<ClientboundExplodePacket> {

    override public void translate(GeyserSession session, ClientboundExplodePacket packet) {
        Vector3f vector = packet.getCenter().toFloat();
        LevelEventGenericPacket levelEventPacket = new LevelEventGenericPacket();
        levelEventPacket.setType(LevelEvent.PARTICLE_BLOCK_EXPLOSION);
        NbtMapBuilder builder = NbtMap.builder();
        builder.putFloat("originX", (float) packet.getCenter().getX());
        builder.putFloat("originY", (float) packet.getCenter().getY());
        builder.putFloat("originZ", (float) packet.getCenter().getZ());




        var particleCreator = JavaLevelParticlesTranslator.createParticle(session, packet.getExplosionParticle());
        if (particleCreator != null) {
            session.sendUpstreamPacket(particleCreator.apply(vector));
        }

        levelEventPacket.setTag(builder.build());
        session.sendUpstreamPacket(levelEventPacket);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        float pitch = (1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f) * 0.7f;
        SoundUtils.playSound(session, packet.getExplosionSound(), vector, 4.0f, pitch);

        if (packet.getPlayerKnockback() != null) {
            SessionPlayerEntity entity = session.getPlayerEntity();
            entity.setMotion(entity.getMotion().add(packet.getPlayerKnockback().toFloat()));

            SetEntityMotionPacket motionPacket = new SetEntityMotionPacket();
            motionPacket.setRuntimeEntityId(entity.geyserId());
            motionPacket.setMotion(entity.getMotion());
            session.sendUpstreamPacket(motionPacket);
        }
    }
}
