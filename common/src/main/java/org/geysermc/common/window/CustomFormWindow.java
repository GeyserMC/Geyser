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

package org.geysermc.common.window;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.common.window.button.FormImage;
import org.geysermc.common.window.component.*;
import org.geysermc.common.window.response.CustomFormResponse;
import org.geysermc.common.window.response.FormResponseData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomFormWindow extends FormWindow {

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private FormImage icon;

    @Getter
    private List<FormComponent> content;

    public CustomFormWindow(String title) {
        this(title, new ArrayList<>());
    }

    public CustomFormWindow(String title, List<FormComponent> content) {
        this(title, content, (FormImage) null);
    }

    public CustomFormWindow(String title, List<FormComponent> content, String icon) {
        this(title, content, new FormImage(FormImage.FormImageType.URL, icon));
    }

    public CustomFormWindow(String title, List<FormComponent> content, FormImage icon) {
        super("custom_form");

        this.title = title;
        this.content = content;
        this.icon = icon;
    }

    public void addComponent(FormComponent component) {
        content.add(component);
    }

    public String getJSONData() {
        String toModify = new Gson().toJson(this);
        //We need to replace this due to Java not supporting declaring class field 'default'
        return toModify.replace("defaultOptionIndex", "default")
                .replace("defaultText", "default")
                .replace("defaultValue", "default")
                .replace("defaultStepIndex", "default");
    }

    public void setResponse(String data) {
        if (data == null || data.equalsIgnoreCase("null") || data.isEmpty()) {
            closed = true;
            return;
        }

        int i = 0;
        Map<Integer, FormResponseData> dropdownResponses = new HashMap<Integer, FormResponseData>();
        Map<Integer, String> inputResponses = new HashMap<Integer, String>();
        Map<Integer, Float> sliderResponses = new HashMap<Integer, Float>();
        Map<Integer, FormResponseData> stepSliderResponses = new HashMap<Integer, FormResponseData>();
        Map<Integer, Boolean> toggleResponses = new HashMap<Integer, Boolean>();
        Map<Integer, Object> responses = new HashMap<Integer, Object>();
        Map<Integer, String> labelResponses = new HashMap<Integer, String>();

        List<String> componentResponses = new Gson().fromJson(data, new TypeToken<List<String>>() { }.getType());
        for (String response : componentResponses) {
            if (i >= content.size()) {
                break;
            }

            FormComponent component = content.get(i);
            if (component == null)
                return;

            if (component instanceof LabelComponent) {
                LabelComponent labelComponent = (LabelComponent) component;
                labelResponses.put(i, labelComponent.getText());
            }

            if (component instanceof DropdownComponent) {
                DropdownComponent dropdownComponent = (DropdownComponent) component;
                String option = dropdownComponent.getOptions().get(Integer.parseInt(response));

                dropdownResponses.put(i, new FormResponseData(Integer.parseInt(response), option));
                responses.put(i, option);
            }

            if (component instanceof InputComponent) {
                inputResponses.put(i, response);
                responses.put(i, response);
            }

            if (component instanceof SliderComponent) {
                float value = Float.parseFloat(response);
                sliderResponses.put(i, value);
                responses.put(i, value);
            }

            if (component instanceof StepSliderComponent) {
                StepSliderComponent stepSliderComponent = (StepSliderComponent) component;
                String step = stepSliderComponent.getSteps().get(Integer.parseInt(response));
                stepSliderResponses.put(i, new FormResponseData(Integer.parseInt(response), step));
                responses.put(i, step);
            }

            if (component instanceof ToggleComponent) {
                boolean answer = Boolean.parseBoolean(response);
                toggleResponses.put(i, answer);
                responses.put(i, answer);
            }
            i++;
        }

        this.response = new CustomFormResponse(responses, dropdownResponses, inputResponses,
                sliderResponses, stepSliderResponses, toggleResponses, labelResponses);
    }
}
