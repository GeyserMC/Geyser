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

package org.geysermc.common.form.impl.component;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.geysermc.common.form.component.ComponentType;
import org.geysermc.common.form.component.DropdownComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Getter
public final class DropdownComponentImpl extends Component implements DropdownComponent {
    private final List<String> options;
    @SerializedName("default")
    private final int defaultOption;

    private DropdownComponentImpl(String text, List<String> options, int defaultOption) {
        super(ComponentType.DROPDOWN, text);
        this.options = Collections.unmodifiableList(options);
        this.defaultOption = defaultOption;
    }

    public static DropdownComponentImpl of(String text, List<String> options, int defaultOption) {
        if (defaultOption == -1 || defaultOption >= options.size()) {
            defaultOption = 0;
        }
        return new DropdownComponentImpl(text, options, defaultOption);
    }

    public static class Builder implements DropdownComponent.Builder {
        private final List<String> options = new ArrayList<>();
        private String text;
        private int defaultOption = 0;

        @Override
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        @Override
        public Builder option(String option, boolean isDefault) {
            options.add(option);
            if (isDefault) {
                defaultOption = options.size() - 1;
            }
            return this;
        }

        @Override
        public Builder option(String option) {
            return option(option, false);
        }

        @Override
        public Builder defaultOption(int defaultOption) {
            this.defaultOption = defaultOption;
            return this;
        }

        @Override
        public DropdownComponentImpl build() {
            return of(text, options, defaultOption);
        }

        @Override
        public DropdownComponentImpl translateAndBuild(Function<String, String> translator) {
            for (int i = 0; i < options.size(); i++) {
                options.set(i, translator.apply(options.get(i)));
            }

            return of(translator.apply(text), options, defaultOption);
        }
    }
}
