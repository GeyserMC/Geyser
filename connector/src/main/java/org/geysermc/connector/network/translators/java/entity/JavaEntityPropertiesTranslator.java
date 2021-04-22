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

package org.geysermc.connector.network.translators.java.entity;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.AttributeUtils;

@Translator(packet = ServerEntityPropertiesPacket.class)
public class JavaEntityPropertiesTranslator extends PacketTranslator<ServerEntityPropertiesPacket> {

    @Override
    public void translate(ServerEntityPropertiesPacket packet, GeyserSession session) {
        boolean isSessionPlayer = false;
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
            isSessionPlayer = true;
        }
        if (entity == null) return;

        for (Attribute attribute : packet.getAttributes()) {
            switch (attribute.getType()) {
                case GENERIC_MAX_HEALTH:
                    entity.getAttributes().put(AttributeType.MAX_HEALTH, AttributeType.MAX_HEALTH.getAttribute((float) AttributeUtils.calculateValue(attribute)));
                    break;
                case GENERIC_ATTACK_DAMAGE:
                    entity.getAttributes().put(AttributeType.ATTACK_DAMAGE, AttributeType.ATTACK_DAMAGE.getAttribute((float) AttributeUtils.calculateValue(attribute)));
                    break;
                case GENERIC_ATTACK_SPEED:
                    if (isSessionPlayer) {
                        // Get attack speed value for use in sending the faux cooldown
                        double attackSpeed = AttributeUtils.calculateValue(attribute);
                        session.setAttackSpeed(attackSpeed);
                    }
                    break;
                case GENERIC_FLYING_SPEED:
                    entity.getAttributes().put(AttributeType.FLYING_SPEED, AttributeType.FLYING_SPEED.getAttribute((float) AttributeUtils.calculateValue(attribute)));
                    entity.getAttributes().put(AttributeType.MOVEMENT_SPEED, AttributeType.MOVEMENT_SPEED.getAttribute((float) AttributeUtils.calculateValue(attribute)));
                    break;
                case GENERIC_MOVEMENT_SPEED:
                    float value = (float) AttributeUtils.calculateValue(attribute);
                    entity.getAttributes().put(AttributeType.MOVEMENT_SPEED, AttributeType.MOVEMENT_SPEED.getAttribute(value));
                    if (isSessionPlayer) {
                        session.setOriginalSpeedAttribute(value);
                        session.adjustSpeed();
                    }
                    break;
                case GENERIC_FOLLOW_RANGE:
                    entity.getAttributes().put(AttributeType.FOLLOW_RANGE, AttributeType.FOLLOW_RANGE.getAttribute((float) AttributeUtils.calculateValue(attribute)));
                    break;
                case GENERIC_KNOCKBACK_RESISTANCE:
                    entity.getAttributes().put(AttributeType.KNOCKBACK_RESISTANCE, AttributeType.KNOCKBACK_RESISTANCE.getAttribute((float) AttributeUtils.calculateValue(attribute)));
                    break;
                case HORSE_JUMP_STRENGTH:
                    entity.getAttributes().put(AttributeType.HORSE_JUMP_STRENGTH, AttributeType.HORSE_JUMP_STRENGTH.getAttribute((float) AttributeUtils.calculateValue(attribute)));
                    break;
            }
        }

        entity.updateBedrockAttributes(session);
    }
}
