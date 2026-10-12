/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.custom;

import org.geysermc.geyser.api.item.custom.v2.CustomItemIcons;
import org.jspecify.annotations.Nullable;

public record GeyserCustomItemIcons(@Nullable String defaultIcon, @Nullable String dyedIcon) implements CustomItemIcons {

    public static class Builder implements CustomItemIcons.Builder {
        private @Nullable String icon;
        private @Nullable String dyedIcon;

        @Override
        public Builder defaultIcon(@Nullable String icon) {
            this.icon = icon;
            return this;
        }

        @Override
        public Builder dyedIcon(@Nullable String dyedIcon) {
            this.dyedIcon = dyedIcon;
            return this;
        }

        @Override
        public CustomItemIcons build() {
            return new GeyserCustomItemIcons(icon, dyedIcon);
        }
    }
}
