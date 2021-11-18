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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.EntityMetadataTranslator;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.InteractiveTagManager;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.List;

@Translator(packet = ClientboundSetEntityDataPacket.class)
public class JavaSetEntityDataTranslator extends PacketTranslator<ClientboundSetEntityDataPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetEntityDataPacket packet) {
        Entity entity;
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        }
        if (entity == null) return;

        List<EntityMetadataTranslator<?, ?>> translators = (List<EntityMetadataTranslator<?, ?>>) entity.getDefinition().translators();

        for (EntityMetadata<?> metadata : packet.getMetadata()) {
            if (metadata.getId() >= translators.size()) {
                session.getConnector().getLogger().warning("Metadata ID " + metadata.getId() + " is out of bounds of known entity metadata size " + translators.size() + " for entity type " + entity.getDefinition().entityType());
                if (session.getConnector().getConfig().isDebugMode()) {
                    session.getConnector().getLogger().debug(metadata.toString());
                }
                continue;
            }

            EntityMetadataTranslator<? super Entity, ?> translator = (EntityMetadataTranslator<? super Entity, ?>) translators.get(metadata.getId());
            if (translator == null) {
                // This can safely happen; it means we don't translate this entity metadata
                continue;
            }
            if (translator.acceptedType() != metadata.getType()) {
                session.getConnector().getLogger().warning("Metadata ID " + metadata.getId() + " was received with type " + metadata.getType() + " but we expected " + translator.acceptedType() + " for " + entity.getDefinition().entityType());
                continue;
            }
            translator.translateFunction().accept(entity, metadata);
        }

        entity.updateBedrockMetadata();

        // Update the interactive tag, if necessary
        if (session.getMouseoverEntity() != null && session.getMouseoverEntity().getEntityId() == entity.getEntityId()) {
            InteractiveTagManager.updateTag(session, entity);
        }
    }
}
