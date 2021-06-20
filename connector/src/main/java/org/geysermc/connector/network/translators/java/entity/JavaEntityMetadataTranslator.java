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

import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import org.geysermc.connector.utils.InteractiveTagManager;
import org.geysermc.connector.utils.LanguageUtils;

@Translator(packet = ServerEntityMetadataPacket.class)
public class JavaEntityMetadataTranslator extends PacketTranslator<ServerEntityMetadataPacket> {

    @Override
    public void translate(ServerEntityMetadataPacket packet, GeyserSession session) {
        Entity entity;
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        }
        if (entity == null) return;

        for (EntityMetadata metadata : packet.getMetadata()) {
            try {
                entity.updateBedrockMetadata(metadata, session);
            } catch (ClassCastException e) {
                // Class cast exceptions are really the only ones we're going to get in normal gameplay
                // Because some entity rewriters forget about some values
                // Any other errors are actual bugs
                session.getConnector().getLogger().warning(LanguageUtils.getLocaleStringLog("geyser.network.translator.metadata.failed", metadata, entity.getEntityType()));
                session.getConnector().getLogger().debug("Entity Java ID: " + entity.getEntityId() + ", Geyser ID: " + entity.getGeyserId());
                if (session.getConnector().getConfig().isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }

        entity.updateBedrockMetadata(session);

        // Update the interactive tag, if necessary
        if (session.getMouseoverEntity() != null && session.getMouseoverEntity().getEntityId() == entity.getEntityId()) {
            InteractiveTagManager.updateTag(session, entity);
        }
    }
}
