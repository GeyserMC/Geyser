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

package org.geysermc.connector.skin;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.translators.PlayerInventoryTranslator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class FakeHeadCache {

    private final GeyserSession session;

    private GeyserItemStack currentHelmet;
    private CompoundTag currentHeadProfile;

    @Getter
    private final Set<UUID> playersWithFakeHeads = new HashSet<>();

    public void addFakeHeadPlayer(UUID uuid) {
        this.playersWithFakeHeads.add(uuid);
    }

    public void removeEntity(Entity entity) {
        if (entity instanceof PlayerEntity) {
            this.playersWithFakeHeads.remove(((PlayerEntity) entity).getUuid());
        }
    }

    public void clear() {
        this.playersWithFakeHeads.clear();
    }

    /**
     * Sets a player's own fake head (or resets it)
     *
     * @param newHelmet the new helmet's itemstack
     * @param profile   the head's profile or <code>null</code> if the new helmet is not a player head with a profile
     * @return <code>true</code> if the item slot setting shouldn't proceed
     */
    public boolean setOwnFakeHead(GeyserItemStack newHelmet, CompoundTag profile) {
        if (!(session.getInventoryTranslator() instanceof PlayerInventoryTranslator) || profile == null) {
            // If profile is null, the player is not wearing a valid player head -> restoring the original skin
            this.currentHelmet = newHelmet;
            this.currentHeadProfile = null;
            FakeHeadProvider.restoreOriginalSkin(session, session.getPlayerEntity());
            return false;
        }

        this.currentHelmet = newHelmet;
        this.currentHeadProfile = profile;

        // Modifying the skin
        FakeHeadProvider.setHead(session, session.getPlayerEntity(), profile);

        // Clearing the item slot if the player inventory isn't opened
        if (session.getOpenInventory() != session.getPlayerInventory()) {
            session.getPlayerInventory().setItem(5, GeyserItemStack.EMPTY, session);
            session.getInventoryTranslator().updateSlot(session, session.getPlayerInventory(), 5);
            return true;
        }

        return false;
    }

    public void onPlayerInventoryOpen() {
        if (this.currentHeadProfile != null) {
            // Setting the item slot to the current head so the player can remove it from the helmet slot
            session.getPlayerInventory().setItem(5, this.currentHelmet, session);
            session.getInventoryTranslator().updateSlot(session, session.getPlayerInventory(), 5);
        }
    }

    public void onPlayerInventoryClose() {
        if (this.currentHeadProfile != null) {
            // Clearing the item slot so the player can see the modified skin
            session.getPlayerInventory().setItem(5, GeyserItemStack.EMPTY, session);
            session.getInventoryTranslator().updateSlot(session, session.getPlayerInventory(), 5);
        }
    }

}
