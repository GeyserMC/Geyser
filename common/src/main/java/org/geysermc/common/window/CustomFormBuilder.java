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

import lombok.Getter;
import org.geysermc.common.window.CustomFormWindow;
import org.geysermc.common.window.button.FormImage;
import org.geysermc.common.window.component.FormComponent;
import org.geysermc.common.window.response.CustomFormResponse;

public class CustomFormBuilder {

    @Getter
    private CustomFormWindow form;

    public CustomFormBuilder(String title) {
        form = new CustomFormWindow(title);
    }

    public CustomFormBuilder setTitle(String title) {
        form.setTitle(title);
        return this;
    }

    public CustomFormBuilder setIcon(FormImage icon) {
        form.setIcon(icon);
        return this;
    }

    public CustomFormBuilder setResponse(String data) {
        form.setResponse(data);
        return this;
    }

    public CustomFormBuilder setResponse(CustomFormResponse response) {
        form.setResponse(response);
        return this;
    }

    public CustomFormBuilder addComponent(FormComponent component) {
        form.addComponent(component);
        return this;
    }

    public CustomFormWindow build() {
        return form;
    }
}
