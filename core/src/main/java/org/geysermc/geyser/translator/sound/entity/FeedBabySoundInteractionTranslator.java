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

package org.geysermc.geyser.translator.sound.entity;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.AnimalEntity;
import org.geysermc.geyser.entity.type.living.animal.OcelotEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.CatEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.sound.EntitySoundInteractionTranslator;
import org.geysermc.geyser.translator.sound.SoundTranslator;

@SoundTranslator
public class FeedBabySoundInteractionTranslator implements EntitySoundInteractionTranslator {

    @Override
    public void translate(GeyserSession session, Vector3f position, Entity entity) {
        if (entity instanceof AnimalEntity animalEntity && !(entity instanceof CatEntity || entity instanceof OcelotEntity)) {
            String handIdentifier = session.getPlayerInventory().getItemInHand().getMapping(session).getJavaIdentifier();
            boolean isBaby = animalEntity.isBaby();
            if (isBaby && animalEntity.canEat(handIdentifier.replace("minecraft:", ""),
                    session.getPlayerInventory().getItemInHand().getMapping(session))) {
                // Play the "feed child" effect
                EntityEventPacket feedEvent = new EntityEventPacket();
                feedEvent.setRuntimeEntityId(entity.getGeyserId());
                feedEvent.setType(EntityEventType.BABY_ANIMAL_FEED);
                session.sendUpstreamPacket(feedEvent);
            }
        }
    }
}
