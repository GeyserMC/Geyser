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

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.util.BlockUtils;

import java.util.Map;

/**
 * Sound interaction handler for when a block is right-clicked.
 */
public interface BlockSoundInteractionTranslator extends SoundInteractionTranslator<String> {

    /**
     * Handles the block interaction when a player
     * right-clicks a block.
     *
     * @param session the session interacting with the block
     * @param position the position of the block
     * @param identifier the identifier of the block
     */
    static void handleBlockInteraction(GeyserSession session, Vector3f position, String identifier) {
        // If we need to get the hand identifier, only get it once and save it to a variable
        String handIdentifier = null;

        for (Map.Entry<SoundTranslator, SoundInteractionTranslator<?>> interactionEntry : Registries.SOUND_TRANSLATORS.get().entrySet()) {
            if (!(interactionEntry.getValue() instanceof BlockSoundInteractionTranslator)) {
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
                if (handIdentifier == null) {
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
            ((BlockSoundInteractionTranslator) interactionEntry.getValue()).translate(session, position, identifier);
        }
    }

    /**
     * Determines if the adventure gamemode would prevent this item from actually succeeding
     */
    static boolean canInteract(GeyserSession session, GeyserItemStack itemInHand, String blockIdentifier) {
        if (session.getGameMode() != GameMode.ADVENTURE) {
            // There are no restrictions on the item
            return true;
        }

        CompoundTag tag = itemInHand.getNbt();
        if (tag == null) {
            // No CanPlaceOn tag can exist
            return false;
        }
        ListTag canPlaceOn = tag.get("CanPlaceOn");
        if (canPlaceOn == null || canPlaceOn.size() == 0) {
            return false;
        }

        String cleanIdentifier = BlockUtils.getCleanIdentifier(blockIdentifier);

        for (Tag t : canPlaceOn) {
            if (t instanceof StringTag stringTag) {
                if (cleanIdentifier.equals(stringTag.getValue())) {
                    // This operation would/could be a success!
                    return true;
                }
            }
        }

        // The block in world is not present in the CanPlaceOn tag on the item
        return false;
    }
}
