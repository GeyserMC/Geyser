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

package org.geysermc.common.form;

import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import org.geysermc.common.form.component.*;
import org.geysermc.common.form.response.CustomFormResponse;
import org.geysermc.common.form.util.FormAdaptor;
import org.geysermc.common.form.util.FormImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@JsonAdapter(FormAdaptor.class)
public final class CustomForm extends Form {
    private final String title;
    private final FormImage icon;
    private final List<Component> content;

    private CustomForm(String title, FormImage icon, List<Component> content) {
        super(Form.Type.CUSTOM_FORM);

        this.title = title;
        this.icon = icon;
        this.content = Collections.unmodifiableList(content);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CustomForm of(String title, FormImage icon, List<Component> content) {
        return new CustomForm(title, icon, content);
    }

    public CustomFormResponse parseResponse(String data) {
        if (isClosed(data)) {
            return CustomFormResponse.closed();
        }
        return CustomFormResponse.of(this, data);
    }

    public static final class Builder extends Form.Builder<Builder, CustomForm> {
        private final List<Component> components = new ArrayList<>();
        private FormImage icon;

        public Builder icon(FormImage.Type type, String data) {
            icon = FormImage.of(type, data);
            return this;
        }

        public Builder iconPath(String path) {
            return icon(FormImage.Type.PATH, path);
        }

        public Builder iconUrl(String url) {
            return icon(FormImage.Type.URL, url);
        }

        public Builder component(Component component) {
            components.add(component);
            return this;
        }

        public Builder dropdown(DropdownComponent.Builder dropdownBuilder) {
            return component(dropdownBuilder.translateAndBuild(this::translate));
        }

        public Builder dropdown(String text, int defaultOption, String... options) {
            List<String> optionsList = new ArrayList<>();
            for (String option : options) {
                optionsList.add(translate(option));
            }
            return component(DropdownComponent.of(translate(text), optionsList, defaultOption));
        }

        public Builder dropdown(String text, String... options) {
            return dropdown(text, -1, options);
        }

        public Builder input(String text, String placeholder, String defaultText) {
            return component(InputComponent.of(
                    translate(text), translate(placeholder), translate(defaultText)
            ));
        }

        public Builder input(String text, String placeholder) {
            return component(InputComponent.of(translate(text), translate(placeholder)));
        }

        public Builder input(String text) {
            return component(InputComponent.of(translate(text)));
        }

        public Builder label(String text) {
            return component(LabelComponent.of(translate(text)));
        }

        public Builder slider(String text, float min, float max, int step, float defaultValue) {
            return component(SliderComponent.of(text, min, max, step, defaultValue));
        }

        public Builder slider(String text, float min, float max, int step) {
            return slider(text, min, max, step, -1);
        }

        public Builder slider(String text, float min, float max, float defaultValue) {
            return slider(text, min, max, -1, defaultValue);
        }

        public Builder slider(String text, float min, float max) {
            return slider(text, min, max, -1, -1);
        }

        public Builder stepSlider(StepSliderComponent.Builder stepSliderBuilder) {
            return component(stepSliderBuilder.translateAndBuild(this::translate));
        }

        public Builder stepSlider(String text, int defaultStep, String... steps) {
            List<String> stepsList = new ArrayList<>();
            for (String option : steps) {
                stepsList.add(translate(option));
            }
            return component(StepSliderComponent.of(translate(text), stepsList, defaultStep));
        }

        public Builder stepSlider(String text, String... steps) {
            return stepSlider(text, -1, steps);
        }

        public Builder toggle(String text, boolean defaultValue) {
            return component(ToggleComponent.of(translate(text), defaultValue));
        }

        public Builder toggle(String text) {
            return component(ToggleComponent.of(translate(text)));
        }

        @Override
        public CustomForm build() {
            CustomForm form = of(title, icon, components);
            if (biResponseHandler != null) {
                form.setResponseHandler(response -> biResponseHandler.accept(form, response));
                return form;
            }

            form.setResponseHandler(responseHandler);
            return form;
        }
    }
}
