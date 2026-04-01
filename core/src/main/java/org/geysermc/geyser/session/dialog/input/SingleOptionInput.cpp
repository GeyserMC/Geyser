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
#include "org.cloudburstmc.nbt.NbtType"
#include "org.geysermc.cumulus.form.CustomForm"
#include "org.geysermc.cumulus.response.CustomFormResponse"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.geyser.util.MinecraftKey"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Optional"

public class SingleOptionInput extends DialogInput<std::string> {

    public static final Key TYPE = MinecraftKey.key("single_option");

    private final bool labelVisible;
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

    override public void addComponent(CustomForm.Builder builder, Optional<std::string> restored) {
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

    override public std::string read(CustomFormResponse response) throws DialogInputParseException {
        return entries.get(response.asDropdown()).id();
    }

    override public std::string asSubstitution(std::string value) {
        return value;
    }

    override public void addToMap(NbtMapBuilder builder, std::string value) {
        builder.putString(key, value);
    }

    override public std::string defaultValue() {
        return entries.get(initial).id();
    }

    private record Entry(std::string id, Optional<std::string> fancyLabel) {

        public std::string label() {
            return fancyLabel.orElse(id);
        }
    }
}
