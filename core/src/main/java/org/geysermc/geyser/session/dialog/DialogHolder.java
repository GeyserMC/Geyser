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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.dialog.action.DialogAction;
import org.geysermc.geyser.session.dialog.input.ParsedInputs;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.Optional;

/**
 * Used to manage an open dialog. Handles dialog input, transferring to other dialogs, and dialog "submenus" for e.g. waiting on a new dialog or confirming a command.
 *
 * <p>This is passed to a {@link Dialog} when using it to send a form to the client, it uses the {@link DialogHolder#runButton(Optional, ParsedInputs)} and {@link DialogHolder#closeDialog(Optional)} methods.</p>
 *
 * <p>
 *     To make it easier to understand what this class does and why it does it, here is the summed up behaviour of dialogs on Java.
 *     Java dialogs consist of inputs and buttons that run actions. Dialogs also have an "on cancel"/closing action, which is usually executed when the user presses ESC, or presses an "exit" button, defined by the dialog
 *     (note: not all dialog types can have an exit button). Dialogs can disallow closing by pressing ESC. Geyser translates clicking the "X" in the corner of a form as pressing ESC.</p>
 *
 * <p>
 *     Dialog inputs are quite simple. The user can enter what they want, and the inputs will clear once the dialog has been closed (note: <em>only</em> once the dialog has been closed. This becomes important later!).
 * </p>
 *
 * <p>
 *     Dialog actions are more complicated. Dialogs can define what to do after an action has been executed (so-called "after action" behaviour). When executing an action, if the action:
 * </p>
 * <ul>
 *    <li>Opens a new dialog: the new dialog is opened, the old one is closed, its closing action not executed.</li>
 *    <li>Executes with "NONE" set as after action: the dialog is kept open, its current input kept. This means the dialog can only be closed by pressing ESC (when allowed), or by an exit button, if it exists.</li>
 *    <li>Executes with "CLOSE" as after action: the dialog is closed, its closing action not executed.</li>
 *    <li>Executes with "WAIT_FOR_RESPONSE" as after action: the dialog is closed, its closing action not executed. A new, temporary screen is opened telling the user Minecraft is waiting on a response from the server.<br>
 *        The server must then send a new dialog within 5 seconds. After this period, a "back" button will appear, allowing the user to go back into the game if no new dialog appears.
 *    </li>
 * </ul>
 *
 * <p>If a new dialog is opened whilst another dialog is open, the old dialog is closed and the new dialog takes its place. The closing action of the dialog is not executed.</p>
 *
 * <p>
 *     All of this behaviour must be emulated by Geyser. That said, here are some of the things that this class must handle:
 * </p>
 *
 * <ul>
 *     <li>Executing actions with after actions properly. Actions that run commands or the "WAIT_FOR_RESPONSE" after action make this especially complicated.<br>
 *         In the case of commands that require operator permissions or the previously mentioned after action, Geyser must open a temporary form asking the user for confirmation or telling the user to wait.
 *     </li>
 *     <li>Remember form input and restore it after returning to this dialog, e.g. by cancelling a command execution or by the "NONE" after action.</li>
 *     <li>
 *         Properly close this dialog and open other dialogs - for example, bedrock/Cumulus likes to call "close" handlers a lot, including when the client closes a currently open form
 *         to open a new one. As such, every time we do something, we must make sure this dialog is still considered open.
 *     </li>
 * </ul>
 *
 * <p>Final note: when reading through this code, a dialog is "valid" when it is still considered open.</p>
 */
@Accessors(fluent = true)
public class DialogHolder {
    @Getter
    private final GeyserSession session;
    private final DialogManager manager;
    private final Dialog dialog;

    /**
     * The time at which the "wait for response" screen was sent. Used to track when to show the "back" button.
     */
    private long responseWaitTime = 0;
    /**
     * If the "wait for response" screen is currently open and the "back" button should be shown.
     */
    private boolean sendBackButton = false;
    /**
     * If the dialog should be closed as soon as possible (likely after a "confirm running command" screen).
     */
    private boolean shouldClose = false;
    private ParsedInputs lastInputs;

    public DialogHolder(GeyserSession session, DialogManager manager, Dialog dialog) {
        this.session = session;
        this.manager = manager;
        this.dialog = dialog;
    }

    /**
     * Checks if this dialog is still valid, and if so, runs the given button (if present) with the given inputs.
     * These inputs can be {@link ParsedInputs#EMPTY} when the dialog has no inputs, but can never be {@code null}. This method also runs the dialog's after action.
     */
    public void runButton(Optional<DialogButton> button, @NonNull ParsedInputs inputs) {
        lastInputs = inputs;
        if (stillValid()) {
            if (runAction(button, lastInputs)) {
                runAfterAction();
            }
        }
    }

    /**
     * Ticks this dialog. Ticks are only used to check when to show the "back" button on the "waiting for response" screen.
     */
    public void tick() {
        // Replace wait form with one with a back button if no replacement dialog was given
        if (responseWaitTime > 0 && !sendBackButton && System.currentTimeMillis() - responseWaitTime > 5000) {
            sendBackButton = true;
            session.closeForm(); // Automatically reopens with a back button
        }
    }

    // Called when clicking the X in the corner on a form, which we interpret as clicking escape
    // Note that this method is called from the "closedOrInvalidResultHandler",
    // meaning it can also be called when e.g. the bedrock client opens another form or is unable to open the form sent to it
    /**
     * Should be called when pressing "ESC", i.e., clicking the X in the corner of the form. This method checks if the dialog is still valid, and if so,
     * closes it if the dialog allows closing by pressing ESC. If not, the dialog is reopened.
     *
     * <p>If the dialog was closed successfully, the given close action is also executed, if present.</p>
     */
    public void closeDialog(Optional<DialogButton> onCancel) {
        if (!stillValid()) {
            return;
        }

        // Don't run close functionality if we're asking for command confirmation
        if (dialog.canCloseWithEscape()) {
            shouldClose = true;
            if (runAction(onCancel, lastInputs == null ? dialog.defaultInputs() : lastInputs)) {
                manager.close();
            }
            return;
        }

        // If player should not have been able to close the dialog, reopen it with the last inputs
        reopenDialog();
    }

    /**
     * Tries to reopen the dialog. First checks if the dialog is still valid. If it is, it checks if the dialog should close,
     * and if so, closes the dialog. If not, the dialog is reopened, with its inputs restored if possible.
     */
    private void reopenDialog() {
        if (stillValid()) {
            if (shouldClose) {
                manager.close();
            } else {
                responseWaitTime = 0;

                // lastInputs might be null here since it's possible none were sent yet
                // Bedrock doesn't send them when just closing the form
                if (lastInputs == null) {
                    dialog.sendForm(this);
                } else {
                    dialog.restoreForm(this, lastInputs);
                }
            }
        }
    }

    /**
     * Runs the dialog's after action. This method assumes the dialog is still valid!
     */
    private void runAfterAction() {
        switch (dialog.afterAction()) {
            case NONE -> {
                // If no new dialog was opened, reopen this one
                dialog.restoreForm(this, lastInputs);
            }
            case CLOSE -> {
                // If no new dialog was opened, tell the manager this one is now closed
                manager.close();
            }
            case WAIT_FOR_RESPONSE -> {
                // If no new dialog was opened, open a form telling the user we're waiting on a response from the server
                // This dialog is replaced with a similar form with a "back" button after 5 seconds, matching Java behaviour
                responseWaitTime = System.currentTimeMillis();
                sendBackButton = false;
                waitForResponse();
            }
        }
    }

    /**
     * Opens a "waiting for response" form. This method assumes the dialog is still valid!
     */
    private void waitForResponse() {
        String content;
        if (sendBackButton) {
            content = GeyserLocale.getPlayerLocaleString("geyser.dialogs.waiting_for_a_while", session.locale());
        } else {
            content = GeyserLocale.getPlayerLocaleString("geyser.dialogs.waiting_for_response", session.locale());
        }

        session.sendDialogForm(SimpleForm.builder()
            .translator(MinecraftLocale::getLocaleString, session.locale())
            .title("gui.waitingForResponse.title")
            .content(content)
            .optionalButton("gui.back", sendBackButton)
            .closedOrInvalidResultHandler(() -> {
                if (stillValid()) { // If still waiting on a new dialog
                    waitForResponse();
                }
            })
            .validResultHandler(response -> manager.close()) // Back button was pressed, meaning no new dialog was sent
            .build());
    }

    /**
     * This method runs the given action, if present, with the given inputs.
     *
     * <p>These inputs can be {@link ParsedInputs#EMPTY} when the dialog has no inputs, but can never be {@code null}.
     * The method returns {@code true} if the dialog's after action can be executed, and {@code false} if not. The latter is the case when the action opened a new
     * dialog or screen, in which case the after action will not be handled or be handled by the screen, respectively.</p>
     *
     * <p>This method assumes the dialog is still valid!</p>
     */
    private boolean runAction(Optional<DialogButton> button, @NonNull ParsedInputs inputs) {
        DialogAction action = button.flatMap(DialogButton::action).orElse(null);
        if (action != null) {
            // Ask the user for confirmation if the dialog wants to run an unknown command or a command that requires operator permissions
            if (action instanceof DialogAction.CommandAction runCommand) {
                String command = runCommand.trimmedCommand(session, inputs);
                String root = command.split(" ")[0];

                // This check is not perfect. Ideally we'd check the entire command and see if any of its arguments require operator permissions, but, that's complicated
                if (session.getRestrictedCommands().contains(root)) {
                    showCommandConfirmation(command, false);
                    return false;
                } else if (!session.getKnownCommands().contains(root)) {
                    showCommandConfirmation(command, true);
                    return false;
                }

                session.sendCommand(command);
                return true;
            } else if (action instanceof DialogAction.OpenUrl openUrl) {
                showUrl(openUrl.url());
                return false;
            } else {
                action.run(session, inputs);
                return !(action instanceof DialogAction.ShowDialog);
            }
        }
        return true;
    }

    /**
     * Opens an "are you sure you want to do this?" form. After confirmation, runs the command and the after action, or closes
     * the dialog if it should be closed. When cancelled, returns back to the dialog, matching Java behaviour. This method assumes the dialog is still valid!
     */
    private void showCommandConfirmation(String trimmedCommand, boolean unknown) {
        Component content = Component.translatable(unknown ? "multiplayer.confirm_command.parse_errors" : "multiplayer.confirm_command.permissions_required",
            Component.text(trimmedCommand).color(NamedTextColor.YELLOW));

        session.sendDialogForm(ModalForm.builder()
            .translator(MinecraftLocale::getLocaleString, session.locale())
            .title("multiplayer.confirm_command.title")
            .content(MessageTranslator.convertMessage(session, content))
            .button1("gui.yes")
            .button2("gui.no")
            .closedOrInvalidResultHandler(() -> {
                // Upon pressing "no" (or closing the form), we should return back to the dialog, even if it was supposed to close
                shouldClose = false;
                // Checks stillValid
                reopenDialog();
            })
            .validResultHandler(response -> {
                // stillValid check not needed here - valid result means the button was pressed, meaning no new dialog took over and closed this form
                if (response.clickedFirst()) {
                    session.sendCommand(trimmedCommand);
                    if (shouldClose) {
                        manager.close();
                    } else {
                        runAfterAction();
                    }
                } else {
                    // Pressed no, go back to dialog
                    shouldClose = false;
                    reopenDialog();
                }
            })
            .build());
    }

    /**
     * Opens a form to let the user know they should open a URL. Runs the after action when closed, or closes the dialog if it should be
     * closed. This method assumes the dialog is still valid!
     */
    private void showUrl(String url) {
        String content = MessageTranslator.convertMessage(session,
                Component.text(GeyserLocale.getPlayerLocaleString("geyser.dialogs.open_url", session.locale()))
                        .append(Component.text("\n\n"))
                        .append(Component.text(url))
                        .append(Component.text("\n\n"))
                        .append(Component.translatable("chat.link.warning").color(NamedTextColor.RED)));

        session.sendDialogForm(SimpleForm.builder()
                .translator(MinecraftLocale::getLocaleString, session.locale())
                .title("chat.link.open")
                .content(content)
                .button("gui.ok")
                .resultHandler((form, result) -> {
                    if (stillValid()) {
                        if (shouldClose) {
                            manager.close();
                        } else {
                            runAfterAction();
                        }
                    }
                })
                .build());
    }

    /**
     * @return true if the dialog currently open is this dialog.
     */
    private boolean stillValid() {
        return manager.open() == this;
    }
}
