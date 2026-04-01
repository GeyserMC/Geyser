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

#include "org.cloudburstmc.nbt.NbtMap"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.dialog.action.DialogAction"
#include "org.geysermc.geyser.translator.text.MessageTranslator"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Optional"

public record DialogButton(std::string label, Optional<DialogAction> action) {

    public static List<DialogButton> readList(Optional<GeyserSession> session, List<NbtMap> tag, Dialog.IdGetter idGetter) {
        if (tag == null) {
            return List.of();
        }
        List<DialogButton> buttons = new ArrayList<>();
        for (NbtMap map : tag) {
            buttons.add(read(session, map, idGetter).orElseThrow());
        }
        return buttons;
    }

    public static Optional<DialogButton> read(Optional<GeyserSession> session, Object tag, Dialog.IdGetter idGetter) {
        if (!(tag instanceof NbtMap map)) {
            return Optional.empty();
        }
        return Optional.of(new DialogButton(MessageTranslator.convertFromNullableNbtTag(session, map.get("label")), DialogAction.read(map.get("action"), idGetter)));
    }
}
