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

public class SliderComponent extends FormComponent {

    @Getter
    @Setter
    private String text;

    @Getter
    @Setter
    private float min;

    @Getter
    @Setter
    private float max;

    @Getter
    @Setter
    private int step;

    @Getter
    @Setter
    private float defaultValue;

    public SliderComponent(String text, float min, float max, int step, float defaultValue) {
        super("slider");

        this.text = text;
        this.min = Math.max(min, 0f);
        this.max = max > this.min ? max : this.min;
        if (step != -1f && step > 0)
            this.step = step;

        if (defaultValue != -1f)
            this.defaultValue = defaultValue;
    }
}
