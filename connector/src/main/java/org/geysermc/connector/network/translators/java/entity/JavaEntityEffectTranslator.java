/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.java.entity;

import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.EntityUtils;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEffectPacket;
import com.nukkitx.protocol.bedrock.packet.MobEffectPacket;

@Translator(packet = ServerEntityEffectPacket.class)
public class JavaEntityEffectTranslator extends PacketTranslator<ServerEntityEffectPacket> {

    @Override
    public void translate(ServerEntityEffectPacket packet, GeyserSession session) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
            ((PlayerEntity) entity).getEffectCache().addEffect(packet.getEffect(), packet.getAmplifier());
        }
        if (entity == null)
            return;

        MobEffectPacket mobEffectPacket = new MobEffectPacket();
        mobEffectPacket.setAmplifier(packet.getAmplifier());
        mobEffectPacket.setDuration(packet.getDuration());
        mobEffectPacket.setEvent(MobEffectPacket.Event.ADD);
        mobEffectPacket.setRuntimeEntityId(entity.getGeyserId());
        mobEffectPacket.setParticles(packet.isShowParticles());
        mobEffectPacket.setEffectId(EntityUtils.toBedrockEffectId(packet.getEffect()));
        session.sendUpstreamPacket(mobEffectPacket);
    }
}
