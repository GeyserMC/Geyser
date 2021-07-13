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

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.connector.entity.attribute.GeyserAttributeType;
import org.geysermc.connector.entity.player.SessionPlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import java.util.Arrays;

@Translator(packet = ServerPlayerSetExperiencePacket.class)
public class JavaPlayerSetExperienceTranslator extends PacketTranslator<ServerPlayerSetExperiencePacket> {

    @Override
    public void translate(ServerPlayerSetExperiencePacket packet, GeyserSession session) {
        SessionPlayerEntity entity = session.getPlayerEntity();

        AttributeData experience = GeyserAttributeType.EXPERIENCE.getAttribute(packet.getExperience());
        entity.getAttributes().put(GeyserAttributeType.EXPERIENCE, experience);
        AttributeData experienceLevel = GeyserAttributeType.EXPERIENCE_LEVEL.getAttribute(packet.getLevel());
        entity.getAttributes().put(GeyserAttributeType.EXPERIENCE_LEVEL, experienceLevel);

        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
        attributesPacket.setAttributes(Arrays.asList(experience, experienceLevel));
        session.sendUpstreamPacket(attributesPacket);
    }
}
