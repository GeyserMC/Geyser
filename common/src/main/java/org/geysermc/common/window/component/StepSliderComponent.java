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

package org.geysermc.common.window.component;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class StepSliderComponent extends FormComponent {

    @Getter
    @Setter
    private String text;

    @Getter
    private List<String> steps;

    @Getter
    @Setter
    private int defaultStepIndex;

    public StepSliderComponent(String text) {
        this(text, new ArrayList<String>());
    }

    public StepSliderComponent(String text, List<String> steps) {
        this(text, steps, 0);
    }

    public StepSliderComponent(String text, List<String> steps, int defaultStepIndex) {
        super("step_slider");

        this.text = text;
        this.steps = steps;
        this.defaultStepIndex = defaultStepIndex;
    }

    public void addStep(String step, boolean isDefault) {
        steps.add(step);

        if (isDefault)
            defaultStepIndex = steps.size() - 1;
    }
}
