/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.geysermc.geyser.api.entity.GeyserEntityDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;

@Getter
@Accessors(fluent = true)
public class BedrockEntityDefinition implements GeyserEntityDefinition {

    private final Identifier identifier;
    private final GeyserEntityProperties registeredProperties;
    private final float width;
    private final float height;
    private final float offset;

    public BedrockEntityDefinition(Identifier identifier, GeyserEntityProperties registeredProperties, float width, float height, float offset) {
        this.identifier = identifier;
        this.registeredProperties = registeredProperties;
        this.width = width;
        this.height = height;
        this.offset = offset;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Identifier identifier;
        private float width;
        private float height;
        private float offset = 0.00001f;
        @Setter(AccessLevel.NONE)
        protected GeyserEntityProperties.Builder propertiesBuilder;

        public Builder() {
        }

        public Builder height(float height) {
            this.height = height;
            return this;
        }

        public Builder width(float width) {
            this.width = width;
            return this;
        }

        public Builder offset(float offset) {
            this.offset = offset + 0.00001f;
            return this;
        }

        public Builder identifier(Identifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder properties(GeyserEntityProperties.Builder propertiesBuilder) {
            this.propertiesBuilder = propertiesBuilder;
            return this;
        }

        BedrockEntityDefinition build() {
            return new BedrockEntityDefinition(identifier, propertiesBuilder != null ? propertiesBuilder.build() : null, width, height, offset);
        }
    }

}
