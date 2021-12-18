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

package org.geysermc.geyser.translator.protocol.java.entity.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddMobPacket;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.factory.BaseEntityFactory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.text.GeyserLocale;

@Translator(packet = ClientboundAddMobPacket.class)
public class JavaAddMobTranslator extends PacketTranslator<ClientboundAddMobPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundAddMobPacket packet) {
        Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        Vector3f motion = Vector3f.from(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());

        EntityDefinition<?> definition = Registries.ENTITY_DEFINITIONS.get(packet.getType());
        if (definition == null) {
            session.getGeyser().getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.entity.type_null", packet.getType()));
            return;
        }

        Entity entity = ((BaseEntityFactory<?>) definition.factory()).create(session, packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(),
                packet.getUuid(), definition, position, motion, packet.getYaw(), packet.getPitch(), packet.getHeadYaw()
        );
        session.getEntityCache().spawnEntity(entity);
    }
}
