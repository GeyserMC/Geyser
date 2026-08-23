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
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MinecraftKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SingleOptionInput extends DialogInput<String> {

    public static final Key TYPE = MinecraftKey.key("single_option");

    private final boolean labelVisible;
    private final List<Entry> entries = new ArrayList<>();
    private final int initial;

    public SingleOptionInput(Optional<GeyserSession> session, NbtMap map) {
        super(session, map);
        labelVisible = map.getBoolean("label_visible", true);
        List<NbtMap> entriesTag = map.getList("options", NbtType.COMPOUND);

        int temp = 0;
        for (int i = 0; i < entriesTag.size(); i++) {
            NbtMap entry = entriesTag.get(i);
            entries.add(new Entry(entry.getString("id"), Optional.ofNullable(MessageTranslator.convertFromNullableNbtTag(session, entry.get("display")))));
            if (entry.getBoolean("initial")) {
                temp = i;
            }
        }
        initial = temp;
    }

    @Override
    public void addComponent(CustomForm.Builder builder, Optional<String> restored) {
        int defaultOption = initial;
        if (restored.isPresent()) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).id().equals(restored.get())) {
                    defaultOption = i;
                    break;
                }
            }
        }
        builder.dropdown(labelVisible ? label : "", entries.stream().map(Entry::label).toList(), defaultOption);
    }

    @Override
    public String read(CustomFormResponse response) throws DialogInputParseException {
        return entries.get(response.asDropdown()).id();
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
        return entries.get(initial).id();
    }

    private record Entry(String id, Optional<String> fancyLabel) {

        public String label() {
            return fancyLabel.orElse(id);
        }
    }
}
