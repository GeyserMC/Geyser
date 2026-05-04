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

import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.dialog.input.ParsedInputs;
import org.geysermc.geyser.util.MinecraftKey;

import java.util.Optional;

public class NoticeDialog extends Dialog {

    public static final Key TYPE = MinecraftKey.key("notice");

    private final Optional<DialogButton> button;

    public NoticeDialog(Optional<GeyserSession> session, NbtMap map, Dialog.IdGetter idGetter) {
        super(session, map);
        button = DialogButton.read(session, map.getCompound("action"), idGetter);
    }

    @Override
    protected Optional<DialogButton> onCancel() {
        return button;
    }

    @Override
    protected void addCustomComponents(DialogHolder holder, CustomForm.Builder builder) {
        builder.validResultHandler(response -> parseInput(holder, response).ifPresent(inputs -> holder.runButton(button, inputs)));
    }

    @Override
    protected void addCustomComponents(DialogHolder holder, SimpleForm.Builder builder) {
        builder.button(button.map(DialogButton::label).orElse("gui.ok"))
            .validResultHandler(response -> holder.runButton(button, ParsedInputs.EMPTY));
    }
}
