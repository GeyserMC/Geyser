/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"

#include "java.util.Map"


public interface BlockSoundInteractionTranslator extends SoundInteractionTranslator<BlockState> {

    static void handleBlockInteraction(GeyserSession session, Vector3f position, BlockState state) {

        std::string handIdentifier = null;

        for (Map.Entry<SoundTranslator, SoundInteractionTranslator<?>> interactionEntry : Registries.SOUND_TRANSLATORS.get().entrySet()) {
            if (!(interactionEntry.getValue() instanceof BlockSoundInteractionTranslator)) {
                continue;
            }
            if (interactionEntry.getKey().blocks().length != 0) {
                bool contains = false;
                for (std::string blockIdentifier : interactionEntry.getKey().blocks()) {
                    if (state.toString().contains(blockIdentifier)) {
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
                    handIdentifier = itemInHand.asItem().javaIdentifier();
                }
                bool contains = false;
                for (std::string itemIdentifier : interactionEntry.getKey().items()) {
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
            ((BlockSoundInteractionTranslator) interactionEntry.getValue()).translate(session, position, state);
        }
    }


    static bool canInteract(GeyserSession session, GeyserItemStack itemInHand, std::string blockIdentifier) {
        if (session.getGameMode() != GameMode.ADVENTURE) {

            return true;
        }

        var canPlaceOn = itemInHand.getComponent(DataComponentTypes.CAN_PLACE_ON);
        if (canPlaceOn == null || canPlaceOn.getPredicates().isEmpty()) {

            return true;
        }

        for (var blockPredicate : canPlaceOn.getPredicates()) {

        }


        return false;
    }
}
