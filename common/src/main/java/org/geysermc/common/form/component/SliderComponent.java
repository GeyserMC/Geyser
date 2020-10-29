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

package org.geysermc.common.form.component;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public final class SliderComponent extends Component {
    private final float min;
    private final float max;
    private final int step;
    @SerializedName("default")
    private final float defaultValue;

    private SliderComponent(String text, float min, float max, int step, float defaultValue) {
        super(Type.SLIDER, text);
        this.min = min;
        this.max = max;
        this.step = step;
        this.defaultValue = defaultValue;
    }

    public static SliderComponent of(String text, float min, float max, int step, float defaultValue) {
        min = Math.max(min, 0f);
        max = Math.max(max, min);

        if (step < 1) {
            step = 1;
        }

        if (defaultValue == -1f) {
            defaultValue = (int) Math.floor(min + max / 2D);
        }

        return new SliderComponent(text, min, max, step, defaultValue);
    }

    public static SliderComponent of(String text, float min, float max, int step) {
        return of(text, min, max, step, -1);
    }

    public static SliderComponent of(String text, float min, float max, float defaultValue) {
        return of(text, min, max, -1, defaultValue);
    }

    public static SliderComponent of(String text, float min, float max) {
        return of(text, min, max, -1, -1);
    }
}
