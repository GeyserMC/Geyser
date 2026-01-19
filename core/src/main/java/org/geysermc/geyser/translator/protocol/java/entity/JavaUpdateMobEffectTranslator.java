/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.level.EffectType;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.EntityEffectCache;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
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

        var event = MobEffectPacket.Event.ADD;

        if (entity == session.getPlayerEntity()) {
            EntityEffectCache cache = session.getEffectCache();
            // Matches BDS
            if (cache.getEntityEffects().contains(packet.getEffect())) {
                event = MobEffectPacket.Event.MODIFY;
            }

            cache.setEffect(packet.getEffect(), packet.getAmplifier());
        } else if (entity instanceof ClientVehicle clientVehicle) {
            clientVehicle.getVehicleComponent().setEffect(packet.getEffect(), packet.getAmplifier());
        }

        MobEffectPacket mobEffectPacket = new MobEffectPacket();
        mobEffectPacket.setAmplifier(packet.getAmplifier());
        mobEffectPacket.setDuration(packet.getDuration());
        mobEffectPacket.setEvent(event);
        mobEffectPacket.setRuntimeEntityId(entity.geyserId());
        mobEffectPacket.setParticles(packet.isShowParticles());
        mobEffectPacket.setAmbient(packet.isAmbient());
        mobEffectPacket.setEffectId(EffectType.fromJavaEffect(packet.getEffect()).getBedrockId());
        session.sendUpstreamPacket(mobEffectPacket);

        // Bedrock expects some attributes to be updated in the same tick as the effect causing them
        if (entity == session.getPlayerEntity()) {
            AttributeData attribute = switch (packet.getEffect()) {
                // Fixes https://github.com/GeyserMC/Geyser/issues/5347
                case ABSORPTION -> session.getPlayerEntity().getAttributes().get(GeyserAttributeType.ABSORPTION);
                // Fixes https://github.com/GeyserMC/Geyser/issues/5388
                case SPEED -> session.getPlayerEntity().getAttributes().get(GeyserAttributeType.MOVEMENT_SPEED);
                default -> null;
            };

            if (attribute == null) {
                return;
            }

            UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
            attributesPacket.setRuntimeEntityId(entity.geyserId());
            attributesPacket.setAttributes(Collections.singletonList(attribute));
            session.sendUpstreamPacket(attributesPacket);
        }
    }
}
