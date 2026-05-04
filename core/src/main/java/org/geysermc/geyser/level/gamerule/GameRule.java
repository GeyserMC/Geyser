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
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
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
     * The category of this gamerule
     */
    GameRuleCategory category();

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

    Component toComponent(GeyserSession session, String currentValue);

    record Int(Key key, GameRuleCategory category, Integer defaultValue, int max, int min) implements GameRule<Integer> {
        public Int(String key, GameRuleCategory category, Integer defaultValue, int max, int min) {
            this(MinecraftKey.key(key), category, defaultValue, max, min);
        }

        @Override
        public TypeAdapter<Integer> adapter() {
            return TypeAdapter.INTEGER;
        }

        @Override
        public boolean validate(Integer value) {
            return value <= max && value >= min;
        }

        @Override
        public Component toComponent(GeyserSession session, String currentValue) {
            return InputComponent.of(GameRule.translate(session, key), currentValue, currentValue);
        }
    }

    record Bool(Key key, GameRuleCategory category, Boolean defaultValue) implements GameRule<Boolean> {
        public Bool(String key, GameRuleCategory category, Boolean defaultValue) {
            this(MinecraftKey.key(key), category, defaultValue);
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
        public Component toComponent(GeyserSession session, String currentValue) {
            return ToggleComponent.of(GameRule.translate(session, key), adapter().parser().apply(currentValue));
        }
    }

    private static String translate(GeyserSession session, Key key) {
        String translatable = "gamerule." + key.namespace() + "." + key.value().replace('/', '.');
        return MessageTranslator.convertMessage(net.kyori.adventure.text.Component.translatable(translatable), session.locale());
    }
}
