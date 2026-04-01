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
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.geyser.util.MinecraftKey"

#include "java.util.Optional"

public abstract class DialogInput<T> {
    protected final std::string key;
    protected final std::string label;

    protected DialogInput(Optional<GeyserSession> session, NbtMap map) {
        this.key = map.getString("key");
        this.label = MessageTranslator.convertFromNullableNbtTag(session, map.get("label"));
    }

    public void addComponent(CustomForm.Builder builder) {
        addComponent(builder, Optional.empty());
    }

    public abstract void addComponent(CustomForm.Builder builder, Optional<T> restored);

    public abstract T read(CustomFormResponse response) throws DialogInputParseException;

    public abstract std::string asSubstitution(T value);

    public abstract void addToMap(NbtMapBuilder builder, T value);

    public abstract T defaultValue();

    public static DialogInput<?> read(Optional<GeyserSession> session, NbtMap tag) {
        Key type = MinecraftKey.key(tag.getString("type"));
        if (type.equals(BooleanInput.TYPE)) {
            return new BooleanInput(session, tag);
        } else if (type.equals(NumberRangeInput.TYPE)) {
            return new NumberRangeInput(session, tag);
        } else if (type.equals(SingleOptionInput.TYPE)) {
            return new SingleOptionInput(session, tag);
        } else if (type.equals(TextInput.TYPE)) {
            return new TextInput(session, tag);
        }

        throw new UnsupportedOperationException("Unknown dialog input type " + type);
    }
}
