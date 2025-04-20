/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.inventory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;

import java.util.List;

/**
 * A helper class storing the current inventory, translator, and session.
 */
@Accessors(fluent = true)
@Getter
public final class InventoryHolder<T extends Inventory> {
    private final GeyserSession session;
    private final T inventory;
    private final InventoryTranslator<T> translator;

    /**
     * Whether this inventory is currently pending.
     * It can be pending if this inventory was opened while another inventory was still open,
     * or because opening this inventory takes more time (e.g. virtual inventories).
     */
    @Setter
    private boolean pending;

    /**
     * Stores the number of attempts to open virtual inventories.
     * Capped at 3, and isn't used in ideal circumstances.
     * Used to resolve <a href="https://github.com/GeyserMC/Geyser/issues/5426">container closing issues.</a>
     */
    @Setter
    private int containerOpenAttempts;

    @SuppressWarnings("unchecked")
    public InventoryHolder(GeyserSession session, Inventory newInventory, InventoryTranslator<? extends Inventory> newTranslator) {
        this.session = session;
        this.inventory = (T) newInventory;
        this.translator = (InventoryTranslator<T>) newTranslator;
    }

    public void markCurrent() {
        this.session.setInventoryHolder(this);
    }

    public boolean shouldSetPending() {
        return session.isClosingInventory() || !session.getUpstream().isInitialized() || session.getPendingOrCurrentBedrockInventoryId() != -1;
    }

    public boolean shouldConfirmClose(boolean confirm) {
        return confirm && inventory.isDisplayed() && !pending;
    }

    public void inheritFromExisting(InventoryHolder<? extends Inventory> existing) {
        // Mirror Bedrock id
        inventory.setBedrockId(existing.bedrockId());

        // Also mirror other properties - in case we're e.g. dealing with a pending virtual inventory
        Inventory existingInventory = existing.inventory;
        this.pending = existing.pending();
        inventory.setDisplayed(existingInventory.isDisplayed());
        inventory.setHolderPosition(existingInventory.getHolderPosition());
        inventory.setHolderId(existingInventory.getHolderId());
        this.markCurrent();
    }

    /*
     * Helper methods to avoid using the wrong translator to update specific inventories.
     */

    public void updateInventory() {
        this.translator.updateInventory(session, inventory);
    }

    public void updateProperty(int rawProperty, int value) {
        this.translator.updateProperty(session, inventory, rawProperty, value);
    }

    public void updateSlot(int slot) {
        this.translator.updateSlot(session, inventory, slot);
    }

    public void openInventory() {
        this.translator.openInventory(session, inventory);
        this.pending = false;
        this.inventory.setDisplayed(true);
    }

    public void closeInventory(boolean force) {
        this.translator.closeInventory(session, inventory, force);
        if (session.getContainerOutputFuture() != null) {
            session.getContainerOutputFuture().cancel(true);
        }
    }

    public boolean requiresOpeningDelay() {
        return this.translator.requiresOpeningDelay(session, inventory);
    }

    public boolean prepareInventory() {
        return this.translator.prepareInventory(session, inventory);
    }

    public void translateRequests(List<ItemStackRequest> requests) {
        this.translator.translateRequests(session, inventory, requests);
    }

    public GeyserSession session() {
        return session;
    }

    public T inventory() {
        return inventory;
    }

    public InventoryTranslator<T> translator() {
        return translator;
    }

    public void incrementContainerOpenAttempts() {
        this.containerOpenAttempts++;
    }

    public int javaId() {
        return inventory.getJavaId();
    }

    public int bedrockId() {
        return inventory.getBedrockId();
    }

    @Override
    public String toString() {
        return "InventoryHolder[" +
            "session=" + session + ", " +
            "inventory=" + inventory + ", " +
            "translator=" + translator + ']';
    }
}
