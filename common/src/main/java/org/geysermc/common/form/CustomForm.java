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

import org.geysermc.common.form.component.Component;
import org.geysermc.common.form.component.DropdownComponent;
import org.geysermc.common.form.component.StepSliderComponent;
import org.geysermc.common.form.impl.CustomFormImpl;
import org.geysermc.common.form.response.CustomFormResponse;

import java.util.List;

public interface CustomForm extends Form<CustomFormResponse> {
    static CustomForm.Builder builder() {
        return new CustomFormImpl.Builder();
    }

    static CustomForm of(String title, FormImage icon, List<Component> content) {
        return CustomFormImpl.of(title, icon, content);
    }

    interface Builder extends FormBuilder<Builder, CustomForm> {
        Builder icon(FormImage.Type type, String data);

        Builder iconPath(String path);

        Builder iconUrl(String url);

        Builder component(Component component);

        Builder dropdown(DropdownComponent.Builder dropdownBuilder);

        Builder dropdown(String text, int defaultOption, String... options);

        Builder dropdown(String text, String... options);

        Builder input(String text, String placeholder, String defaultText);

        Builder input(String text, String placeholder);

        Builder input(String text);

        Builder label(String text);

        Builder slider(String text, float min, float max, int step, float defaultValue);

        Builder slider(String text, float min, float max, int step);

        Builder slider(String text, float min, float max, float defaultValue);

        Builder slider(String text, float min, float max);

        Builder stepSlider(StepSliderComponent.Builder stepSliderBuilder);

        Builder stepSlider(String text, int defaultStep, String... steps);

        Builder stepSlider(String text, String... steps);

        Builder toggle(String text, boolean defaultValue);

        Builder toggle(String text);
    }
}
