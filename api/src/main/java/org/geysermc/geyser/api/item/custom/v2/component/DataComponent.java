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

package org.geysermc.geyser.api.item.custom.v2.component;

import org.geysermc.geyser.api.util.Identifier;

import java.util.function.Predicate;

public final class DataComponent<T> {
    public static final DataComponent<Consumable> CONSUMABLE = create("consumable");
    public static final DataComponent<Equippable> EQUIPPABLE = create("equippable");
    public static final DataComponent<FoodProperties> FOOD = create("food");
    /**
     * Must be at or above 0.
     */
    public static final DataComponent<Integer> MAX_DAMAGE = create("max_damage", i -> i >= 0);
    /**
     * Must be between 1 and 99.
     */
    public static final DataComponent<Integer> MAX_STACK_SIZE = create("max_stack_size", i -> i >= 1 && i <= 99); // Reverse lambda
    public static final DataComponent<UseCooldown> USE_COOLDOWN = create("use_cooldown");
    /**
     * Must be at or above 0.
     *
     * <p>Note that, on Bedrock, this will be mapped to the {@code minecraft:enchantable} component with {@code slot=all}. This should, but does not guarantee,
     * allow for compatibility with vanilla enchantments. Non-vanilla enchantments are unlikely to work.</p>
     */
    public static final DataComponent<Integer> ENCHANTABLE = create("enchantable", i -> i >= 0);
    /**
     * At the moment only used for the {@link ToolProperties#canDestroyBlocksInCreative()} option, which will be a feature in Java 1.21.5, but is already in use in Geyser.
     *
     * <p>Like other components, when not set this will fall back to the default value.</p>
     */
    public static final DataComponent<ToolProperties> TOOL = create("tool");

    private final Identifier identifier;
    private final Predicate<T> validator;

    private DataComponent(Identifier identifier, Predicate<T> validator) {
        this.identifier = identifier;
        this.validator = validator;
    }

    private static <T> DataComponent<T> create(String name) {
        return new DataComponent<>(new Identifier(Identifier.DEFAULT_NAMESPACE, name), t -> true);
    }

    private static <T> DataComponent<T> create(String name, Predicate<T> validator) {
        return new DataComponent<>(new Identifier(Identifier.DEFAULT_NAMESPACE, name), validator);
    }

    public boolean validate(T value) {
        return validator.test(value);
    }

    @Override
    public String toString() {
        return "data component " + identifier.toString();
    }
}
