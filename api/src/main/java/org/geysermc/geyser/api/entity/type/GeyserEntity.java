/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.entity.type;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.entity.property.BatchWriter;
import org.geysermc.geyser.api.entity.property.GeyserBooleanEntityProperty;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.entity.property.GeyserIntEntityProperty;
import org.geysermc.geyser.api.entity.property.PropertyBatch;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a unique instance of an entity. Each {@link org.geysermc.geyser.api.connection.GeyserConnection}
 * have their own sets of entities - no two instances will share the same GeyserEntity instance.
 */
public interface GeyserEntity {
    /**
     * @return the entity ID that the server has assigned to this entity.
     */
    @NonNegative
    int javaId();

    @NonNull
    <T> Map<GeyserEntityProperty<T>, T> properties();

    PropertyBatch updateProperties();

    void updatePropertiesBatched(Consumer<BatchWriter> consumer);

    default void usageTest() {
        GeyserBooleanEntityProperty someBoolProperty = new GeyserBooleanEntityProperty() {
            @Override
            public @NonNull String name() {
                return "";
            }

            @Override
            public @NonNull Boolean defaultValue() {
                return null;
            }

            @Override
            public void updateValue(@NonNull GeyserEntity entity, @NonNull Boolean value) {

            }
        };

        GeyserIntEntityProperty someIntProperty = new GeyserIntEntityProperty() {

            @Override
            public @NonNull String name() {
                return "";
            }

            @Override
            public @NonNull Integer defaultValue() {
                return 0;
            }

            @Override
            public void updateValue(@NonNull GeyserEntity entity, @NonNull Integer value) {

            }

            @Override
            public int min() {
                return 0;
            }

            @Override
            public int max() {
                return 0;
            }
        };

        this.updateProperties()
            .set(someBoolProperty, false)
            .set(someIntProperty, 3)
            .send();

        this.updatePropertiesBatched(consumer -> {
            consumer.set(someBoolProperty, false);
            consumer.set(someIntProperty, 2);
        });
    }
}
