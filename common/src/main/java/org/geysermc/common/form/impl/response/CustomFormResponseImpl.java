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

package org.geysermc.common.form.impl.response;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.geysermc.common.form.component.Component;
import org.geysermc.common.form.component.ComponentType;
import org.geysermc.common.form.impl.CustomFormImpl;
import org.geysermc.common.form.response.CustomFormResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class CustomFormResponseImpl implements CustomFormResponse {
    private static final Gson GSON = new Gson();
    private static final CustomFormResponseImpl CLOSED =
            new CustomFormResponseImpl(true, false, null, null);
    private static final CustomFormResponseImpl INVALID =
            new CustomFormResponseImpl(false, true, null, null);
    private final boolean closed;
    private final boolean invalid;

    private final JsonArray responses;
    private final List<ComponentType> componentTypes;

    private int index = -1;

    public static CustomFormResponseImpl closed() {
        return CLOSED;
    }

    public static CustomFormResponseImpl invalid() {
        return INVALID;
    }

    public static CustomFormResponseImpl of(CustomFormImpl form, String responseData) {
        JsonArray responses = GSON.fromJson(responseData, JsonArray.class);
        List<ComponentType> types = new ArrayList<>();
        for (Component component : form.getContent()) {
            types.add(component.getType());
        }
        return of(types, responses);
    }

    public static CustomFormResponseImpl of(List<ComponentType> componentTypes,
                                            JsonArray responses) {
        if (componentTypes.size() != responses.size()) {
            return invalid();
        }

        return new CustomFormResponseImpl(false, false, responses,
                Collections.unmodifiableList(componentTypes));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T next(boolean includeLabels) {
        if (!hasNext()) {
            return null;
        }

        while (++index < responses.size()) {
            ComponentType type = componentTypes.get(index);
            if (type == ComponentType.LABEL && !includeLabels) {
                continue;
            }
            return (T) getDataFromType(type, index);
        }
        return null; // we don't have anything to check anymore
    }

    @Override
    public <T> T next() {
        return next(false);
    }

    @Override
    public void skip(int amount) {
        index += amount;
    }

    @Override
    public void skip() {
        skip(1);
    }

    @Override
    public void index(int index) {
        this.index = index;
    }

    @Override
    public boolean hasNext() {
        return responses.size() > index + 1;
    }

    @Override
    public JsonPrimitive get(int index) {
        try {
            return responses.get(index).getAsJsonPrimitive();
        } catch (IllegalStateException exception) {
            wrongType(index, "a primitive");
            return null;
        }
    }

    @Override
    public int getDropdown(int index) {
        JsonPrimitive primitive = get(index);
        if (!primitive.isNumber()) {
            wrongType(index, "dropdown");
        }
        return primitive.getAsInt();
    }

    @Override
    public String getInput(int index) {
        JsonPrimitive primitive = get(index);
        if (!primitive.isString()) {
            wrongType(index, "input");
        }
        return primitive.getAsString();
    }

    @Override
    public float getSlider(int index) {
        JsonPrimitive primitive = get(index);
        if (!primitive.isNumber()) {
            wrongType(index, "slider");
        }
        return primitive.getAsFloat();
    }

    @Override
    public int getStepSlide(int index) {
        JsonPrimitive primitive = get(index);
        if (!primitive.isNumber()) {
            wrongType(index, "step slider");
        }
        return primitive.getAsInt();
    }

    @Override
    public boolean getToggle(int index) {
        JsonPrimitive primitive = get(index);
        if (!primitive.isBoolean()) {
            wrongType(index, "toggle");
        }
        return primitive.getAsBoolean();
    }

    private Object getDataFromType(ComponentType type, int index) {
        switch (type) {
            case DROPDOWN:
                return getDropdown(index);
            case INPUT:
                return getInput(index);
            case SLIDER:
                return getSlider(index);
            case STEP_SLIDER:
                return getStepSlide(index);
            case TOGGLE:
                return getToggle(index);
            default:
                return null; // label e.g. is always null
        }
    }

    private void wrongType(int index, String expected) {
        throw new IllegalStateException(String.format(
                "Expected %s on %s, got %s",
                expected, index, responses.get(index).toString()));
    }
}
