/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.gamerule;

import net.kyori.adventure.key.Key;
import org.geysermc.cumulus.component.Component;
import org.geysermc.cumulus.component.InputComponent;
import org.geysermc.cumulus.component.ToggleComponent;
import org.geysermc.geyser.util.MinecraftKey;

/**
 * This enum stores each gamerule along with the value type and the default.
 * It is used to construct the list for the settings menu
 */
public interface GameRule<T> {

    /**
     * The id of the gamerule; without the minecraft namespace
     */
    Key key();

    /**
     * temp: the translation of the gamerule
     * TODO remove once we properly parse / handle deprecations in locale files
     */
    String translation();

    /**
     * magic
     */
    TypeAdapter<T> adapter();

    /**
     * default value for the gamerule
     */
    T defaultValue();

    /**
     * ensures gamerule values are within range
     */
    boolean validate(T value);

    Component toComponent(String currentValue);

    record Int(Key key, String translation, Integer defaultValue, int min, int max) implements GameRule<Integer> {

        public Int(String key, String translation, Integer defaultValue, int min, int max) {
            this(MinecraftKey.key(key), translation, defaultValue, min, max);
        }

        @Override
        public TypeAdapter<Integer> adapter() {
            return TypeAdapter.INTEGER;
        }

        @Override
        public boolean validate(Integer value) {
            return value >= min && value <= max;
        }

        @Override
        public Component toComponent(String currentValue) {
            return InputComponent.of(translation(), currentValue, currentValue);
        }
    }

    record Bool(Key key, String translation, Boolean defaultValue) implements GameRule<Boolean> {

        public Bool(String key, String translation, Boolean defaultValue) {
            this(MinecraftKey.key(key), translation, defaultValue);
        }

        @Override
        public TypeAdapter<Boolean> adapter() {
            return TypeAdapter.BOOLEAN;
        }

        @Override
        public boolean validate(Boolean value) {
            return value != null;
        }

        @Override
        public Component toComponent(String currentValue) {
            return ToggleComponent.of(translation(), adapter().parser().apply(currentValue));
        }
    }
}
