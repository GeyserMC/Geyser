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

package org.geysermc.geyser.translator.protocol.java.entity;

import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityMotionPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket;

@Translator(packet = ClientboundSetEntityMotionPacket.class)
public class JavaSetEntityMotionTranslator extends PacketTranslator<ClientboundSetEntityMotionPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetEntityMotionPacket packet) {
        Entity entity;
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        }
        if (entity == null) return;

        entity.setMotion(Vector3f.from(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ()));

        if (entity == session.getRidingVehicleEntity() && entity instanceof AbstractHorseEntity) {
            // Horses for some reason teleport back when a SetEntityMotionPacket is sent while
            // a player is riding on them. Java clients seem to ignore it anyways.
            return;
        }

        if (entity instanceof ItemEntity) {
            // Don't bother sending entity motion packets for items
            // since the client doesn't seem to care
            return;
        }

        SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
        entityMotionPacket.setRuntimeEntityId(entity.getGeyserId());
        entityMotionPacket.setMotion(entity.getMotion());

        session.sendUpstreamPacket(entityMotionPacket);
    }
}
