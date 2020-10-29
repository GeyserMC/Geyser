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

package org.geysermc.common.form.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.geysermc.common.form.CustomForm;
import org.geysermc.common.form.Form;
import org.geysermc.common.form.ModalForm;
import org.geysermc.common.form.SimpleForm;
import org.geysermc.common.form.component.ButtonComponent;
import org.geysermc.common.form.component.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FormAdaptor implements JsonDeserializer<Form>, JsonSerializer<Form> {
    private static final Type LIST_BUTTON_TYPE =
            new TypeToken<List<ButtonComponent>>() {}.getType();

    @Override
    public Form deserialize(JsonElement jsonElement, Type typeOfT,
                            JsonDeserializationContext context)
            throws JsonParseException {

        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("Form has to be a JsonObject");
        }
        JsonObject json = jsonElement.getAsJsonObject();

        if (typeOfT == SimpleForm.class) {
            String title = json.get("title").getAsString();
            String content = json.get("content").getAsString();
            List<ButtonComponent> buttons = context
                    .deserialize(json.get("buttons"), LIST_BUTTON_TYPE);
            return SimpleForm.of(title, content, buttons);
        }

        if (typeOfT == ModalForm.class) {
            String title = json.get("title").getAsString();
            String content = json.get("content").getAsString();
            String button1 = json.get("button1").getAsString();
            String button2 = json.get("button2").getAsString();
            return ModalForm.of(title, content, button1, button2);
        }

        if (typeOfT == CustomForm.class) {
            String title = json.get("title").getAsString();
            FormImage icon = context.deserialize(json.get("icon"), FormImage.class);
            List<Component> content = new ArrayList<>();

            JsonArray contentArray = json.getAsJsonArray("content");
            for (JsonElement contentElement : contentArray) {
                String typeName = contentElement.getAsJsonObject().get("type").getAsString();

                Component.Type type = Component.Type.getByName(typeName);
                if (type == null) {
                    throw new JsonParseException("Failed to find Component type " + typeName);
                }

                content.add(context.deserialize(contentElement, type.getComponentClass()));
            }
            return CustomForm.of(title, icon, content);
        }
        return null;
    }

    @Override
    public JsonElement serialize(Form src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add("type", context.serialize(src.getType()));

        if (typeOfSrc == SimpleForm.class) {
            SimpleForm form = (SimpleForm) src;

            result.addProperty("title", form.getTitle());
            result.addProperty("content", form.getContent());
            result.add("buttons", context.serialize(form.getButtons(), LIST_BUTTON_TYPE));
            return result;
        }

        if (typeOfSrc == ModalForm.class) {
            ModalForm form = (ModalForm) src;

            result.addProperty("title", form.getTitle());
            result.addProperty("content", form.getContent());
            result.addProperty("button1", form.getButton1());
            result.addProperty("button2", form.getButton2());
            return result;
        }

        if (typeOfSrc == CustomForm.class) {
            CustomForm form = (CustomForm) src;

            result.addProperty("title", form.getTitle());
            result.add("icon", context.serialize(form.getIcon()));
            result.add("content", context.serialize(form.getContent()));
            return result;
        }
        return null;
    }
}
