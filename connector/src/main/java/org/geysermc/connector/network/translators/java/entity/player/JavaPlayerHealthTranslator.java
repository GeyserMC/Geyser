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

package org.geysermc.connector.network.translators.java.entity.player;

import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.nukkitx.protocol.bedrock.packet.SetHealthPacket;

@Translator(packet = ServerPlayerHealthPacket.class)
public class JavaPlayerHealthTranslator extends PacketTranslator<ServerPlayerHealthPacket> {

    @Override
    public void translate(ServerPlayerHealthPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        int health = (int) Math.ceil(packet.getHealth());
        SetHealthPacket setHealthPacket = new SetHealthPacket();
        setHealthPacket.setHealth(health);
        session.sendUpstreamPacket(setHealthPacket);

        float maxHealth = entity.getAttributes().containsKey(AttributeType.MAX_HEALTH) ? entity.getAttributes().get(AttributeType.MAX_HEALTH).getValue() : 20f;
        // Max health must be divisible by two in bedrock
        if ((maxHealth % 2) == 1) {
            maxHealth += 1;
        }

        entity.getAttributes().put(AttributeType.HEALTH, AttributeType.HEALTH.getAttribute(health, maxHealth));
        entity.getAttributes().put(AttributeType.HUNGER, AttributeType.HUNGER.getAttribute(packet.getFood()));
        entity.getAttributes().put(AttributeType.SATURATION, AttributeType.SATURATION.getAttribute(packet.getSaturation()));
        entity.updateBedrockAttributes(session);
    }
}
