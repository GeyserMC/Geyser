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
import lombok.Getter;
import lombok.Setter;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.common.window.response.SimpleFormResponse;

import java.util.ArrayList;
import java.util.List;


public class SimpleFormWindow extends FormWindow {

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String content;

    @Getter
    @Setter
    private List<FormButton> buttons;

    public SimpleFormWindow(String title, String content) {
        this(title, content, new ArrayList<FormButton>());
    }

    public SimpleFormWindow(String title, String content, List<FormButton> buttons) {
        super("form");

        this.title = title;
        this.content = content;
        this.buttons = buttons;
    }

    @Override
    public String getJSONData() {
        return new Gson().toJson(this);
    }

    public void setResponse(String data) {
        if (data == null || data.equalsIgnoreCase("null")) {
            closed = true;
            return;
        }

        int buttonID;
        try {
            buttonID = Integer.parseInt(data);
        } catch (Exception ex) {
            return;
        }

        if (buttonID >= buttons.size()) {
            response = new SimpleFormResponse(buttonID, null);
            return;
        }

        response = new SimpleFormResponse(buttonID, buttons.get(buttonID));
    }
}
