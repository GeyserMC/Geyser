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

package org.geysermc.connector.network.translators.sound;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemRegistry;

import java.util.Map;

/**
 * Sound interaction handler for when a block is right-clicked.
 */
public interface BlockSoundInteractionHandler extends SoundInteractionHandler<String> {

    /**
     * Handles the block interaction when a player
     * right-clicks a block.
     *
     * @param session the session interacting with the block
     * @param position the position of the block
     * @param identifier the identifier of the block
     */
    static void handleBlockInteraction(GeyserSession session, Vector3f position, String identifier) {
        for (Map.Entry<SoundHandler, SoundInteractionHandler<?>> interactionEntry : SoundHandlerRegistry.INTERACTION_HANDLERS.entrySet()) {
            if (!(interactionEntry.getValue() instanceof BlockSoundInteractionHandler)) {
                continue;
            }
            if (interactionEntry.getKey().blocks().length != 0) {
                boolean contains = false;
                for (String blockIdentifier : interactionEntry.getKey().blocks()) {
                    if (identifier.contains(blockIdentifier)) {
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
                String handIdentifier = itemInHand.getItemEntry().getJavaIdentifier();
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
            ((BlockSoundInteractionHandler) interactionEntry.getValue()).handleInteraction(session, position, identifier);
        }
    }
}
