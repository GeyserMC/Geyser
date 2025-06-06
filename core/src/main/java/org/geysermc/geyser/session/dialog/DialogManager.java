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

package org.geysermc.geyser.session.dialog;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;

/**
 * Small class to manage the currently open dialog.
 */
@Accessors(fluent = true)
public class DialogManager {
    private final GeyserSession session;
    @Getter
    private DialogHolder open;

    public DialogManager(GeyserSession session) {
        this.session = session;
    }

    public void openDialog(Key dialog) {
        openDialog(Dialog.getDialogFromKey(session, dialog));
    }

    public void openDialog(Holder<NbtMap> dialog) {
        openDialog(Dialog.getDialogFromHolder(session, dialog));
    }

    /**
     * Opens a new dialog. If a dialog was already open, this one will be closed. Its closing action will not be executed. This matches Java behaviour.
     */
    public void openDialog(Dialog dialog) {
        open = new DialogHolder(session, this, dialog);
        session.closeForm();
        dialog.sendForm(open);
    }

    public void tick() {
        if (open != null) {
            open.tick();
        }
    }

    /**
     * Closes the currently open dialog, if any. The dialog's closing action will not be executed.
     */
    public void close() {
        if (open != null) {
            open = null;
            // The form could already have been closed by now, but in the case it wasn't, close it anyway
            // This won't run a closing dialog action, because the manager already regards the dialog as closed
            session.closeForm();
        }
    }
}
