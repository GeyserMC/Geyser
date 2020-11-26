/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.common.form.impl;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.common.form.Form;
import org.geysermc.common.form.FormBuilder;
import org.geysermc.common.form.FormType;
import org.geysermc.common.form.Forms;
import org.geysermc.common.form.response.FormResponse;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Getter
public abstract class FormImpl<T extends FormResponse> implements Form<T> {
    protected static final Gson GSON = Forms.getGson();

    private final FormType type;
    protected String hardcodedJsonData = null;
    @Setter protected Consumer<String> responseHandler;

    public FormImpl(FormType type) {
        this.type = type;
    }

    @Override
    public String getJsonData() {
        if (hardcodedJsonData != null) {
            return hardcodedJsonData;
        }
        return GSON.toJson(this);
    }

    @Override
    public boolean isClosed(String response) {
        return response == null || response.isEmpty() || response.equalsIgnoreCase("null");
    }

    public static abstract class Builder<T extends FormBuilder<T, F>, F extends Form<?>>
            implements FormBuilder<T, F> {

        protected String title = "";

        protected BiFunction<String, String, String> translationHandler = null;
        protected BiConsumer<F, String> biResponseHandler;
        protected Consumer<String> responseHandler;
        protected String locale;

        @Override
        public T title(String title) {
            this.title = translate(title);
            return self();
        }

        @Override
        public T translator(BiFunction<String, String, String> translator, String locale) {
            this.translationHandler = translator;
            this.locale = locale;
            return title(title);
        }

        @Override
        public T translator(BiFunction<String, String, String> translator) {
            return translator(translator, locale);
        }

        @Override
        public T responseHandler(BiConsumer<F, String> responseHandler) {
            biResponseHandler = responseHandler;
            return self();
        }

        @Override
        public T responseHandler(Consumer<String> responseHandler) {
            this.responseHandler = responseHandler;
            return self();
        }

        @Override
        public abstract F build();

        protected String translate(String text) {
            if (translationHandler != null && text != null && !text.isEmpty()) {
                return translationHandler.apply(text, locale);
            }
            return text;
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }
    }
}
