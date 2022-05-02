/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.custom.items.builders;

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.custom.CustomRenderOffsets;
import org.geysermc.geyser.api.custom.items.CustomItemData;
import org.geysermc.geyser.api.custom.items.CustomItemRegistrationTypes;

public record GeyserCustomItemData(String name,
                                   CustomItemRegistrationTypes registrationTypes,
                                   String displayName, boolean allowOffhand, int textureSize,
                                   CustomRenderOffsets renderOffsets) implements CustomItemData {

    public static class Builder implements CustomItemData.Builder {
        private final String name;
        private final CustomItemRegistrationTypes registrationType;

        private String displayName;
        private boolean allowOffhand;
        private int textureSize;
        private CustomRenderOffsets renderOffsets;

        public Builder(@NonNull String name, @NonNull CustomItemRegistrationTypes registrationType) {
            this.registrationType = registrationType;
            this.name = name;

            this.displayName = name;
            this.allowOffhand = false;
            this.textureSize = 16;

            this.renderOffsets = null;
        }

        @Override
        public CustomItemData.Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public CustomItemData.Builder allowOffhand(boolean allowOffhand) {
            this.allowOffhand = allowOffhand;
            return this;
        }

        @Override
        public CustomItemData.Builder textureSize(int textureSize) {
            this.textureSize = textureSize;
            return this;
        }

        @Override
        public CustomItemData.Builder renderOffsets(CustomRenderOffsets renderOffsets) {
            this.renderOffsets = renderOffsets;
            return this;
        }

        @Override
        public CustomItemData build() {
            return new GeyserCustomItemData(this.name, this.registrationType, this.displayName, this.allowOffhand, this.textureSize, this.renderOffsets);
        }
    }
}
