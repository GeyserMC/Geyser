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

package org.geysermc.geyser.translator.sound;

import com.nukkitx.math.vector.Vector3f;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.Registries;

import java.util.Map;

/**
 * Sound interaction handler for when an entity is right-clicked.
 */
public interface EntitySoundInteractionTranslator extends SoundInteractionTranslator<Entity> {

    /**
     * Handles the block interaction when a player
     * right-clicks an entity.
     *
     * @param session the session interacting with the block
     * @param position the position of the block
     * @param entity the entity interacted with
     */
    static void handleEntityInteraction(GeyserSession session, Vector3f position, Entity entity) {
        // If we need to get the hand identifier, only get it once and save it to a variable
        String handIdentifier = null;

        for (Map.Entry<SoundTranslator, SoundInteractionTranslator<?>> interactionEntry : Registries.SOUND_TRANSLATORS.get().entrySet()) {
            if (!(interactionEntry.getValue() instanceof EntitySoundInteractionTranslator)) {
                continue;
            }
            if (interactionEntry.getKey().entities().length != 0) {
                boolean contains = false;
                for (String entityIdentifier : interactionEntry.getKey().entities()) {
                    if (entity.getDefinition().entityType().name().toLowerCase().contains(entityIdentifier)) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) continue;
            }
            GeyserItemStack itemInHand = session.getPlayerInventory().getItemInHand();
            if (interactionEntry.getKey().items().length != 0) {
                if (itemInHand.isEmpty()) {
                    continue;
                }
                if (handIdentifier == null) {
                    // Don't get the identifier unless we need it
                    handIdentifier = itemInHand.getMapping(session).getJavaIdentifier();
                }
                boolean contains = false;
                for (String itemIdentifier : interactionEntry.getKey().items()) {
                    if (handIdentifier.contains(itemIdentifier)) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) continue;
            }
            if (session.isSneaking() && !interactionEntry.getKey().ignoreSneakingWhileHolding()) {
                if (!itemInHand.isEmpty()) {
                    continue;
                }
            }
            ((EntitySoundInteractionTranslator) interactionEntry.getValue()).translate(session, position, entity);
        }
    }
}
