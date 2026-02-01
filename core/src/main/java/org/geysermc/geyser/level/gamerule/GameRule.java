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

/**
 * This enum stores each gamerule along with the value type and the default.
 * It is used to construct the list for the settings menu
 */
public interface GameRule<T> {
    String id();
    // TODO yeet once we properly parse / handle deprecations in locale files
    String translation();
    TypeAdapter<T> adapter();
    T defaultValue();
    boolean validate(T value);

    record Int(String id, String translation, Integer defaultValue, int min, int max) implements GameRule<Integer> {
        @Override
        public TypeAdapter<Integer> adapter() {
            return TypeAdapter.INTEGER;
        }

        @Override
        public boolean validate(Integer value) {
            return value >= min && value <= max;
        }
    }

    record Bool(String id, String translation, Boolean defaultValue) implements GameRule<Boolean> {
        @Override
        public TypeAdapter<Boolean> adapter() {
            return TypeAdapter.BOOLEAN;
        }

        @Override
        public boolean validate(Boolean value) {
            return value != null;
        }
    }
}
