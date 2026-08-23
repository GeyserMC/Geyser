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

import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MinecraftKey;

import java.util.Optional;

public class TextInput extends DialogInput<String> {

    public static final Key TYPE = MinecraftKey.key("text");

    private final boolean labelVisible;
    private final String initial;
    private final int maxLength;

    public TextInput(Optional<GeyserSession> session, NbtMap map) {
        super(session, map);
        labelVisible = map.getBoolean("label_visible", true);
        initial = map.getString("initial", "");
        maxLength = map.getInt("max_length", 32);
    }

    @Override
    public void addComponent(CustomForm.Builder builder, Optional<String> restored) {
        builder.input(labelVisible ? label : "", "", restored.orElse(initial));
    }

    @Override
    public String read(CustomFormResponse response) throws DialogInputParseException {
        String text = response.asInput();
        assert text != null;
        if (text.length() > maxLength) {
            throw new DialogInputParseException("geyser.dialogs.text_input_limit", text, maxLength);
        }
        return text;
    }

    @Override
    public String asSubstitution(String value) {
        return value;
    }

    @Override
    public void addToMap(NbtMapBuilder builder, String value) {
        builder.putString(key, value);
    }

    @Override
    public String defaultValue() {
        return initial;
    }
}
