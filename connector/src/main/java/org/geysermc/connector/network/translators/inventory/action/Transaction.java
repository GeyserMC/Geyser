/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.inventory.action;

import lombok.Getter;
import lombok.ToString;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A transaction is created when changes are made to the Inventory. This will store changes sent to us from
 * downstream and playback a series of actions.
 */

@Getter
@ToString(onlyExplicitlyIncluded = true)
public class Transaction {
    public static final List<Transaction> TRANSACTIONS = new ArrayList<>();
    public static Transaction CURRENT_TRANSACTION = null;

    @ToString.Include
    private final PriorityQueue<BaseAction> actions = new PriorityQueue<>();

    @ToString.Include
    private BaseAction currentAction = null;

    private final GeyserSession session;
    private final InventoryTranslator translator;
    private final Inventory inventory;

    private static boolean running = false;

    private Transaction(GeyserSession session, InventoryTranslator translator, Inventory inventory) {
        this.session = session;
        this.translator = translator;
        this.inventory = inventory;
    }

    public void add(BaseAction action) {
        action.setTransaction(this);
        actions.add(action);
    }

    /**
     * Start Transactions
     */
    void start() {
        if (actions.isEmpty()) {
            nextTransaction();
            return;
        }
        next();
    }

    /**
     * Execute the next action
     */
    public void next() {
        if (actions.isEmpty()) {
            currentAction = null;
            nextTransaction();
            return;
        }

        currentAction = actions.remove();
        currentAction.execute();
    }

    public static Transaction of(GeyserSession session, InventoryTranslator translator, Inventory inventory) {
        Transaction ret = new Transaction(session, translator, inventory);
        TRANSACTIONS.add(ret);
        return ret;
    }

    /**
     * Start Execution of Transactions if not already started
     */
    public static void execute() {
        if (running || TRANSACTIONS.isEmpty()) {
            return;
        }

        running = true;

        nextTransaction();
    }

    public static void nextTransaction() {
        if (TRANSACTIONS.isEmpty()) {
            CURRENT_TRANSACTION = null;
            running = false;
            return;
        }

        CURRENT_TRANSACTION = TRANSACTIONS.remove(0);
        CURRENT_TRANSACTION.start();
    }

    public static void cancel() {
        running = false;
        TRANSACTIONS.clear();
        CURRENT_TRANSACTION = null;
    }

}
