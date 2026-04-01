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


@Accessors(fluent = true)
public class DialogHolder {
    @Getter
    private final GeyserSession session;
    private final DialogManager manager;
    private final Dialog dialog;

    
    private long responseWaitTime = 0;
    
    private boolean sendBackButton = false;
    
    private boolean shouldClose = false;
    private ParsedInputs lastInputs;

    public DialogHolder(GeyserSession session, DialogManager manager, Dialog dialog) {
        this.session = session;
        this.manager = manager;
        this.dialog = dialog;
    }

    
    public void runButton(Optional<DialogButton> button, @NonNull ParsedInputs inputs) {
        lastInputs = inputs;
        if (stillValid()) {
            if (runAction(button, lastInputs)) {
                runAfterAction();
            }
        }
    }

    
    public void tick() {
        
        if (responseWaitTime > 0 && !sendBackButton && System.currentTimeMillis() - responseWaitTime > 5000) {
            sendBackButton = true;
            session.closeForm(); 
        }
    }

    
    
    
    
    public void closeDialog(Optional<DialogButton> onCancel) {
        if (!stillValid()) {
            return;
        }

        
        if (dialog.canCloseWithEscape()) {
            shouldClose = true;
            if (runAction(onCancel, lastInputs == null ? dialog.defaultInputs() : lastInputs)) {
                manager.close();
            }
            return;
        }

        
        reopenDialog();
    }

    
    private void reopenDialog() {
        if (stillValid()) {
            if (shouldClose) {
                manager.close();
            } else {
                responseWaitTime = 0;

                
                
                if (lastInputs == null) {
                    dialog.sendForm(this);
                } else {
                    dialog.restoreForm(this, lastInputs);
                }
            }
        }
    }

    
    private void runAfterAction() {
        switch (dialog.afterAction()) {
            case NONE -> {
                
                dialog.restoreForm(this, lastInputs);
            }
            case CLOSE -> {
                
                manager.close();
            }
            case WAIT_FOR_RESPONSE -> {
                
                
                responseWaitTime = System.currentTimeMillis();
                sendBackButton = false;
                waitForResponse();
            }
        }
    }

    
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
                if (stillValid()) { 
                    waitForResponse();
                }
            })
            .validResultHandler(response -> manager.close()) 
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
            
            if (action instanceof DialogAction.CommandAction runCommand) {
                String command = runCommand.trimmedCommand(session, inputs);
                String root = command.split(" ")[0];

                
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
                
                shouldClose = false;
                
                reopenDialog();
            })
            .validResultHandler(response -> {
                
                if (response.clickedFirst()) {
                    session.sendCommand(trimmedCommand);
                    if (shouldClose) {
                        manager.close();
                    } else {
                        runAfterAction();
                    }
                } else {
                    
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
