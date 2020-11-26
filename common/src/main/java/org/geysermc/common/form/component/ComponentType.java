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
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ComponentType {
    @SerializedName("dropdown")
    DROPDOWN(DropdownComponent.class),
    @SerializedName("input")
    INPUT(InputComponent.class),
    @SerializedName("label")
    LABEL(LabelComponent.class),
    @SerializedName("slider")
    SLIDER(SliderComponent.class),
    @SerializedName("step_slider")
    STEP_SLIDER(StepSliderComponent.class),
    @SerializedName("toggle")
    TOGGLE(ToggleComponent.class);

    private static final ComponentType[] VALUES = values();

    private final String name = name().toLowerCase();
    private final Class<? extends Component> componentClass;

    public static ComponentType getByName(String name) {
        for (ComponentType type : VALUES) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
