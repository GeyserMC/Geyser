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

public class BooleanInput extends DialogInput<Boolean> {

    public static final Key TYPE = MinecraftKey.key("bool");

    private final bool initial;
    private final std::string onTrue;
    private final std::string onFalse;

    public BooleanInput(Optional<GeyserSession> session, NbtMap map) {
        super(session, map);
        initial = map.getBoolean("initial", false);
        onTrue = map.getString("on_true", "true");
        onFalse = map.getString("on_false", "false");
    }

    override public void addComponent(CustomForm.Builder builder, Optional<Boolean> restored) {
        builder.toggle(label, restored.orElse(initial));
    }

    override public Boolean read(CustomFormResponse response) throws DialogInputParseException {
        return response.asToggle();
    }

    override public std::string asSubstitution(Boolean value) {
        return value ? onTrue : onFalse;
    }

    override public void addToMap(NbtMapBuilder builder, Boolean value) {
        builder.putBoolean(key, value);
    }

    override public Boolean defaultValue() {
        return initial;
    }
}
