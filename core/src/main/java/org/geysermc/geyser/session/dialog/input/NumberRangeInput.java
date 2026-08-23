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

public class NumberRangeInput extends DialogInput<Float> {

    public static final Key TYPE = MinecraftKey.key("number_range");

    private final float start;
    private final float end;
    private final float initial;
    private final float step;

    public NumberRangeInput(Optional<GeyserSession> session, NbtMap map) {
        super(session, map);
        start = map.getFloat("start");
        end = map.getFloat("end");
        initial = map.getFloat("initial", start + (end - start) / 2.0F);
        step = map.getFloat("step", 1.0F);
    }

    @Override
    public void addComponent(CustomForm.Builder builder, Optional<Float> restored) {
        // Note: Java uses live labels which change as you change the slider, showing the value. This is what the "label_format" key is for. This is not supported on bedrock.
        builder.slider(label, start, end, step, restored.orElse(initial));
    }

    @Override
    public Float read(CustomFormResponse response) throws DialogInputParseException {
        return response.asSlider();
    }

    @Override
    public String asSubstitution(Float value) {
        int rounded = value.intValue();
        return (float) rounded == value ? Integer.toString(rounded) : Float.toString(value);
    }

    @Override
    public void addToMap(NbtMapBuilder builder, Float value) {
        builder.putFloat(key, value);
    }

    @Override
    public Float defaultValue() {
        return initial;
    }
}
