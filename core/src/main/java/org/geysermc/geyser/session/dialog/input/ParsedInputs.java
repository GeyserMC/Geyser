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

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.geyser.session.dialog.DialogHolder;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParsedInputs {
    public static final ParsedInputs EMPTY = new ParsedInputs(List.of());

    private final Map<DialogInput<?>, Object> values = new LinkedHashMap<>();
    private final Map<DialogInput<?>, DialogInputParseException> errors = new HashMap<>();

    public ParsedInputs(List<DialogInput<?>> inputs, CustomFormResponse response) {
        for (DialogInput<?> input : inputs) {
            try {
                values.put(input, input.read(response));
            } catch (DialogInputParseException exception) {
                values.put(input, exception.getPartial());
                errors.put(input, exception);
            }
        }
    }

    public ParsedInputs(List<DialogInput<?>> inputs) {
        for (DialogInput<?> input : inputs) {
            values.put(input, input.defaultValue());
        }
    }

    public void restore(DialogHolder holder, CustomForm.Builder builder) {
        for (Map.Entry<DialogInput<?>, Object> entry : values.entrySet()) {
            DialogInputParseException exception = errors.get(entry.getKey());
            if (exception != null) {
                String formattedException = GeyserLocale.getPlayerLocaleString(exception.getMessage(), holder.session().locale(), exception.getValues());
                String error = GeyserLocale.getPlayerLocaleString("geyser.dialogs.input_validation_error", holder.session().locale(), formattedException);

                builder.label("§c" + error);
                builder.label("§c" + GeyserLocale.getPlayerLocaleString("geyser.dialogs.input_adjust", holder.session().locale()));
            }
            // Can't be a Geyser update without eclipse dealing with generics
            ((DialogInput) entry.getKey()).addComponent(builder, Optional.of(entry.getValue()));
        }
    }

    public String getSubstitution(String key) {
        for (Map.Entry<DialogInput<?>, Object> entry : values.entrySet()) {
            if (entry.getKey().key.equals(key)) {
                return ((DialogInput) entry.getKey()).asSubstitution(entry.getValue());
            }
        }
        return ""; // Java defaults to empty strings when a key was not in the inputs
    }

    public NbtMap asNbtMap() {
        NbtMapBuilder builder = NbtMap.builder();
        for (Map.Entry<DialogInput<?>, Object> entry : values.entrySet()) {
            ((DialogInput) entry.getKey()).addToMap(builder, entry.getValue());
        }
        return builder.build();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
