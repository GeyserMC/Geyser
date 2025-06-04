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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.session.dialog.action.DialogAction;
import org.geysermc.geyser.session.dialog.input.ParsedInputs;
import org.geysermc.geyser.text.MinecraftLocale;

import java.util.Optional;

public class DialogHolder {
    private final DialogManager manager;
    private final Dialog dialog;
    private long responseWaitTime = 0;
    private boolean sendBackButton = false;
    private ParsedInputs lastInputs;

    public DialogHolder(DialogManager manager, Dialog dialog) {
        this.manager = manager;
        this.dialog = dialog;
    }


    // Dialog action behaviour:
    // When running an action, if the action:
    // - opens a new dialog: the new dialog is opened, the old one is closed, its closing action not executed. [ ]
    // - executes with "none" as after action: the dialog is kept open. The dialog can only be closed by pressing "escape" (when allowed by the dialog), or on bedrock, the X in the corner of the form. [ ] (TODO note about multi action exit)
    // - executes with "close" as after action: the dialog is closed, its closing action not executed.
    // A dialogs closing action (onCancel()) is only executed when the dialog is closed by pressing "escape" (when allowed by the dialog). [ ]
    // If a new dialog is opened by the server when a dialog is already open, that dialog is closed, its closing action not executed. [ ]

    public void runButton(Optional<DialogButton> button, @NonNull ParsedInputs inputs) {
        lastInputs = inputs;

        boolean stillValid = runAction(button, lastInputs);
        switch (dialog.afterAction()) {
            case NONE -> {
                // If no new dialog was opened, reopen this one
                if (stillValid) {
                    dialog.restoreForm(manager.session(), lastInputs, this);
                }
            }
            case CLOSE -> {
                // If no new dialog was opened, tell the manager this one is now closed
                if (stillValid) {
                    manager.close();
                }
            }
            case WAIT_FOR_RESPONSE -> {
                // If no new dialog was opened, open a form telling the user we're waiting on a response from the server
                // This dialog is replaced with a similar form with a "back" button after 5 seconds, matching Java behaviour
                if (stillValid) {
                    responseWaitTime = System.currentTimeMillis();
                    sendBackButton = false;
                    waitForResponse();
                }
            }
        }
    }

    public void tick() {
        // Replace wait form with one with a back button if no replacement dialog was given
        if (responseWaitTime > 0 && !sendBackButton && System.currentTimeMillis() - responseWaitTime > 5000) {
            sendBackButton = true;
            manager.session().closeForm(); // Automatically reopens with a back button
        }
    }

    // Called when clicking the X in the corner on a form, which we interpret as clicking escape
    // Note that this method is called from the "closedOrInvalidResultHandler",
    // meaning it can also be called when e.g. the bedrock client opens another form or is unable to open the form sent to it
    public void closeDialog(Optional<DialogButton> onCancel) {
        if (dialog.canCloseWithEscape()) {
            if (runAction(onCancel, lastInputs == null ? ParsedInputs.EMPTY : lastInputs)) {
                manager.close();
            }
        } else if (manager.open() == this) { // Check if this is still the currently open dialog
            // If player should not have been able to close the dialog, reopen it with the last inputs
            reopenDialog();
        }
    }

    private void reopenDialog() {
        responseWaitTime = 0;

        // lastInputs might be null here since it's possible none were sent yet
        // Bedrock doesn't send them when just closing the form
        if (lastInputs == null) {
            dialog.sendForm(manager.session(), this);
        } else {
            dialog.restoreForm(manager.session(), lastInputs, this);
        }
    }

    private void waitForResponse() {
        String content = "Geyser is waiting for the server to respond with a new dialog.";
        if (sendBackButton) {
            content += " The server is taking a while to respond. You can press the button below to go back to the game.";
        } else {
            content += " If no new dialog is shown within 5 seconds, a button will appear to go back to the game.";
        }

        manager.session().sendDialogForm(SimpleForm.builder()
            .translator(MinecraftLocale::getLocaleString, manager.session().locale())
            .title("gui.waitingForResponse.title")
            .content(content)
            .optionalButton("Back", sendBackButton)
            .closedOrInvalidResultHandler(() -> {
                if (manager.open() == this) { // If still waiting on a new dialog
                    waitForResponse();
                }
            })
            .validResultHandler(response -> manager.close()) // Back button was pressed, meaning no new dialog was sent
            .build());
    }

    // Returns true if this dialog is still regarded open by the DialogManager
    // When returning false, that means a new dialog has been opened, possibly by the action, which takes this dialog's place
    private boolean runAction(Optional<DialogButton> button, ParsedInputs inputs) {
        // Don't run any action if a new dialog has already been opened
        if (manager.open() != this) {
            return false;
        }

        DialogAction action = button.flatMap(DialogButton::action).orElse(null);
        if (action != null) {
            action.run(manager.session(), inputs);
            if (action instanceof DialogAction.ShowDialog) {
                return false;
            }
            // TODO command warning screen
        }
        return true;
    }
}
