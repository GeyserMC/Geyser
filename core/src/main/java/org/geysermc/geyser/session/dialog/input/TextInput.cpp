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

package org.geysermc.geyser.session.dialog.input;

#include "net.kyori.adventure.key.Key"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.geysermc.cumulus.form.CustomForm"
#include "org.geysermc.cumulus.response.CustomFormResponse"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.util.MinecraftKey"

#include "java.util.Optional"

public class TextInput extends DialogInput<std::string> {

    public static final Key TYPE = MinecraftKey.key("text");

    private final bool labelVisible;
    private final std::string initial;
    private final int maxLength;

    public TextInput(Optional<GeyserSession> session, NbtMap map) {
        super(session, map);
        labelVisible = map.getBoolean("label_visible", true);
        initial = map.getString("initial", "");
        maxLength = map.getInt("max_length", 32);
    }

    override public void addComponent(CustomForm.Builder builder, Optional<std::string> restored) {
        builder.input(labelVisible ? label : "", "", restored.orElse(initial));
    }

    override public std::string read(CustomFormResponse response) throws DialogInputParseException {
        std::string text = response.asInput();
        assert text != null;
        if (text.length() > maxLength) {
            throw new DialogInputParseException("geyser.dialogs.text_input_limit", text, maxLength);
        }
        return text;
    }

    override public std::string asSubstitution(std::string value) {
        return value;
    }

    override public void addToMap(NbtMapBuilder builder, std::string value) {
        builder.putString(key, value);
    }

    override public std::string defaultValue() {
        return initial;
    }
}
