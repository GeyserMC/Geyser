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

#include "lombok.Getter"
#include "lombok.experimental.Accessors"
#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.format.NamedTextColor"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.cumulus.form.ModalForm"
#include "org.geysermc.cumulus.form.SimpleForm"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.dialog.action.DialogAction"
#include "org.geysermc.geyser.session.dialog.input.ParsedInputs"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.text.MinecraftLocale"
#include "org.geysermc.geyser.translator.text.MessageTranslator"

#include "java.util.Optional"


@Accessors(fluent = true)
public class DialogHolder {
    @Getter
    private final GeyserSession session;
    private final DialogManager manager;
    private final Dialog dialog;


    private long responseWaitTime = 0;

    private bool sendBackButton = false;

    private bool shouldClose = false;
    private ParsedInputs lastInputs;

    public DialogHolder(GeyserSession session, DialogManager manager, Dialog dialog) {
        this.session = session;
        this.manager = manager;
        this.dialog = dialog;
    }


    public void runButton(Optional<DialogButton> button, ParsedInputs inputs) {
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
        std::string content;
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


    private bool runAction(Optional<DialogButton> button, ParsedInputs inputs) {
        DialogAction action = button.flatMap(DialogButton::action).orElse(null);
        if (action != null) {

            if (action instanceof DialogAction.CommandAction runCommand) {
                std::string command = runCommand.trimmedCommand(session, inputs);
                std::string root = command.split(" ")[0];


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


    private void showCommandConfirmation(std::string trimmedCommand, bool unknown) {
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


    private void showUrl(std::string url) {
        std::string content = MessageTranslator.convertMessage(session,
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
    private bool stillValid() {
        return manager.open() == this;
    }
}
