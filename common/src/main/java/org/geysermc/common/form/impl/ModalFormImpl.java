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
import org.geysermc.common.form.FormType;
import org.geysermc.common.form.ModalForm;
import org.geysermc.common.form.impl.response.ModalFormResponseImpl;
import org.geysermc.common.form.impl.util.FormAdaptor;
import org.geysermc.common.form.response.ModalFormResponse;

@Getter
@JsonAdapter(FormAdaptor.class)
public final class ModalFormImpl extends FormImpl<ModalFormResponse> implements ModalForm {
    private final String title;
    private final String content;
    private final String button1;
    private final String button2;

    private ModalFormImpl(String title, String content, String button1, String button2) {
        super(FormType.MODAL_FORM);

        this.title = title;
        this.content = content;
        this.button1 = button1;
        this.button2 = button2;
    }

    public static ModalFormImpl of(String title, String content, String button1, String button2) {
        return new ModalFormImpl(title, content, button1, button2);
    }

    public ModalFormResponse parseResponse(String data) {
        if (isClosed(data)) {
            return ModalFormResponseImpl.closed();
        }
        data = data.trim();

        if ("true".equals(data)) {
            return ModalFormResponseImpl.of(0, button1);
        } else if ("false".equals(data)) {
            return ModalFormResponseImpl.of(1, button2);
        }
        return ModalFormResponseImpl.invalid();
    }

    public static final class Builder extends FormImpl.Builder<ModalForm.Builder, ModalForm>
            implements ModalForm.Builder {

        private String content = "";
        private String button1 = "";
        private String button2 = "";

        public Builder content(String content) {
            this.content = translate(content);
            return this;
        }

        public Builder button1(String button1) {
            this.button1 = translate(button1);
            return this;
        }

        public Builder button2(String button2) {
            this.button2 = translate(button2);
            return this;
        }

        @Override
        public ModalForm build() {
            ModalFormImpl form = of(title, content, button1, button2);
            if (biResponseHandler != null) {
                form.setResponseHandler(response -> biResponseHandler.accept(form, response));
                return form;
            }

            form.setResponseHandler(responseHandler);
            return form;
        }
    }
}
