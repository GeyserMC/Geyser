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

package org.geysermc.geyser.translator.protocol.java.entity;

import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;

import java.util.Collections;

@Translator(packet = ClientboundUpdateMobEffectPacket.class)
public class JavaUpdateMobEffectTranslator extends PacketTranslator<ClientboundUpdateMobEffectPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundUpdateMobEffectPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (entity == null) {
            return;
        }

        if (entity == session.getPlayerEntity()) {
            session.getEffectCache().setEffect(packet.getEffect(), packet.getAmplifier());
        } else if (entity instanceof ClientVehicle clientVehicle) {
            clientVehicle.getVehicleComponent().setEffect(packet.getEffect(), packet.getAmplifier());
        }

        MobEffectPacket mobEffectPacket = new MobEffectPacket();
        mobEffectPacket.setAmplifier(packet.getAmplifier());
        mobEffectPacket.setDuration(packet.getDuration());
        mobEffectPacket.setEvent(MobEffectPacket.Event.ADD);
        mobEffectPacket.setRuntimeEntityId(entity.getGeyserId());
        mobEffectPacket.setParticles(packet.isShowParticles());
        mobEffectPacket.setEffectId(EntityUtils.toBedrockEffectId(packet.getEffect()));
        session.sendUpstreamPacket(mobEffectPacket);

        // Fixes https://github.com/GeyserMC/Geyser/issues/5347 by re-sending the correct absorption hearts
        if (entity == session.getPlayerEntity() && packet.getEffect() == Effect.ABSORPTION) {
            var absorptionAttribute = session.getPlayerEntity().getAttributes().get(GeyserAttributeType.ABSORPTION);
            if (absorptionAttribute == null) {
                return;
            }

            UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
            attributesPacket.setRuntimeEntityId(entity.getGeyserId());
            // Setting to a higher maximum since plugins/datapacks can probably extend the Bedrock soft limit
            attributesPacket.setAttributes(Collections.singletonList(
                GeyserAttributeType.ABSORPTION.getAttribute(absorptionAttribute.getValue())));
            session.sendUpstreamPacket(attributesPacket);
        }
    }
}
