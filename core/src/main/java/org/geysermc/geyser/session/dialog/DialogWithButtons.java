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

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.cumulus.component.DropdownComponent;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.dialog.input.ParsedInputs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class DialogWithButtons extends Dialog {

    protected final List<DialogButton> buttons;
    protected final Optional<DialogButton> exitAction;

    protected DialogWithButtons(GeyserSession session, NbtMap map, List<DialogButton> buttons, Optional<DialogButton> exitAction) {
        super(session, map);
        this.buttons = buttons;
        this.exitAction = exitAction;
    }

    @Override
    protected void addCustomComponents(GeyserSession session, CustomForm.Builder builder, DialogHolder holder) {
        DropdownComponent.Builder dropdown = DropdownComponent.builder();
        dropdown.text("Please select an option:");
        for (DialogButton button : buttons) {
            dropdown.option(button.label());
        }
        exitAction.ifPresent(button -> dropdown.option(button.label()));
        builder.dropdown(dropdown);

        builder.validResultHandler(response -> {
            parseInput(session, response, holder).ifPresent(inputs -> {
                int selection = response.asDropdown();
                if (selection == buttons.size()) {
                    holder.runButton(exitAction, inputs);
                } else {
                    holder.runButton(Optional.of(buttons.get(selection)), inputs);
                }
            });
        });
    }

    @Override
    protected void addCustomComponents(GeyserSession session, SimpleForm.Builder builder, DialogHolder holder) {
        for (DialogButton button : buttons) {
            builder.button(button.label());
        }
        exitAction.ifPresent(button -> builder.button(button.label()));

        builder.validResultHandler(response -> {
            if (response.clickedButtonId() == buttons.size()) {
                holder.runButton(exitAction, ParsedInputs.EMPTY);
            } else {
                holder.runButton(Optional.of(buttons.get(response.clickedButtonId())), ParsedInputs.EMPTY);
            }
        });
    }

    @Override
    protected Optional<DialogButton> onCancel() {
        return exitAction;
    }

    @SafeVarargs
    protected static List<DialogButton> parseOptionalList(Optional<DialogButton>... buttons) {
        List<DialogButton> checked = new ArrayList<>();
        for (Optional<DialogButton> button : buttons) {
            checked.add(button.orElseThrow());
        }
        return checked;
    }
}
