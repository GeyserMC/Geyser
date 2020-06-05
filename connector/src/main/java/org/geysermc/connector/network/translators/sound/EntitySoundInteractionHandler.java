/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.sound;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemRegistry;

import java.util.Map;

/**
 * Sound interaction handler for when an entity is right-clicked.
 */
public interface EntitySoundInteractionHandler extends SoundInteractionHandler<Entity> {

    /**
     * Handles the block interaction when a player
     * right-clicks an entity.
     *
     * @param session the session interacting with the block
     * @param position the position of the block
     * @param entity the entity interacted with
     */
    static void handleEntityInteraction(GeyserSession session, Vector3f position, Entity entity) {
        for (Map.Entry<SoundHandler, SoundInteractionHandler<?>> interactionEntry : SoundHandlerRegistry.INTERACTION_HANDLERS.entrySet()) {
            if (!(interactionEntry.getValue() instanceof EntitySoundInteractionHandler)) {
                continue;
            }
            if (interactionEntry.getKey().entities().length != 0) {
                boolean contains = false;
                for (String entityIdentifier : interactionEntry.getKey().entities()) {
                    if (entity.getEntityType().name().toLowerCase().contains(entityIdentifier)) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) continue;
            }
            ItemStack itemInHand = session.getInventory().getItemInHand();
            if (interactionEntry.getKey().items().length != 0) {
                if (itemInHand == null || itemInHand.getId() == 0) {
                    continue;
                }
                String handIdentifier = ItemRegistry.getItem(session.getInventory().getItemInHand()).getJavaIdentifier();
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
                if (session.getInventory().getItemInHand() != null && session.getInventory().getItemInHand().getId() != 0) {
                    continue;
                }
            }
            ((EntitySoundInteractionHandler) interactionEntry.getValue()).handleInteraction(session, position, entity);
        }
    }
}
