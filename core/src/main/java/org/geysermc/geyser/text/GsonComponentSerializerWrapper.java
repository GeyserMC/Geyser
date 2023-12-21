/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.text;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.UnaryOperator;

/**
 * A wrapper around a normal GsonComponentSerializer to accept null components.
 */
public record GsonComponentSerializerWrapper(GsonComponentSerializer source) implements GsonComponentSerializer {

    @Override
    public @NonNull Gson serializer() {
        return this.source.serializer();
    }

    @Override
    public @NonNull UnaryOperator<GsonBuilder> populator() {
        return this.source.populator();
    }

    @Override
    public @NonNull Component deserializeFromTree(@NonNull JsonElement input) {
        // This has yet to be an issue, so it won't be overridden unless we have to
        return this.source.deserializeFromTree(input);
    }

    @Override
    public @NonNull JsonElement serializeToTree(@NonNull Component component) {
        return this.source.serializeToTree(component);
    }

    @Override
    public @NonNull Component deserialize(@NonNull String input) {
        // See https://github.com/KyoriPowered/adventure/issues/447
        Component component = this.serializer().fromJson(input, Component.class);
        if (component == null) {
            return Component.empty();
        }

        return component;
    }

    @Override
    public @NonNull String serialize(@NonNull Component component) {
        return this.source.serialize(component);
    }

    @Override
    public @NonNull Builder toBuilder() {
        return this.source.toBuilder();
    }
}
