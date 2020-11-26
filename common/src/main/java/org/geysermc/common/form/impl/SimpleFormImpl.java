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

import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import org.geysermc.common.form.FormImage;
import org.geysermc.common.form.FormType;
import org.geysermc.common.form.SimpleForm;
import org.geysermc.common.form.component.ButtonComponent;
import org.geysermc.common.form.impl.component.ButtonComponentImpl;
import org.geysermc.common.form.impl.response.SimpleFormResponseImpl;
import org.geysermc.common.form.impl.util.FormAdaptor;
import org.geysermc.common.form.response.SimpleFormResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@JsonAdapter(FormAdaptor.class)
public final class SimpleFormImpl extends FormImpl<SimpleFormResponse> implements SimpleForm {
    private final String title;
    private final String content;
    private final List<ButtonComponent> buttons;

    private SimpleFormImpl(String title, String content, List<ButtonComponent> buttons) {
        super(FormType.SIMPLE_FORM);

        this.title = title;
        this.content = content;
        this.buttons = Collections.unmodifiableList(buttons);
    }

    public static SimpleFormImpl of(String title, String content, List<ButtonComponent> buttons) {
        return new SimpleFormImpl(title, content, buttons);
    }

    public SimpleFormResponse parseResponse(String data) {
        if (isClosed(data)) {
            return SimpleFormResponseImpl.closed();
        }
        data = data.trim();

        int buttonId;
        try {
            buttonId = Integer.parseInt(data);
        } catch (Exception exception) {
            return SimpleFormResponseImpl.invalid();
        }

        if (buttonId >= buttons.size()) {
            return SimpleFormResponseImpl.invalid();
        }

        return SimpleFormResponseImpl.of(buttonId, buttons.get(buttonId));
    }

    public static final class Builder extends FormImpl.Builder<SimpleForm.Builder, SimpleForm>
            implements SimpleForm.Builder {

        private final List<ButtonComponent> buttons = new ArrayList<>();
        private String content = "";

        public Builder content(String content) {
            this.content = translate(content);
            return this;
        }

        public Builder button(String text, FormImage.Type type, String data) {
            buttons.add(ButtonComponentImpl.of(translate(text), type, data));
            return this;
        }

        public Builder button(String text, FormImage image) {
            buttons.add(ButtonComponentImpl.of(translate(text), image));
            return this;
        }

        public Builder button(String text) {
            buttons.add(ButtonComponentImpl.of(translate(text)));
            return this;
        }

        @Override
        public SimpleForm build() {
            SimpleFormImpl form = of(title, content, buttons);
            if (biResponseHandler != null) {
                form.setResponseHandler(response -> biResponseHandler.accept(form, response));
                return form;
            }

            form.setResponseHandler(responseHandler);
            return form;
        }
    }
}
