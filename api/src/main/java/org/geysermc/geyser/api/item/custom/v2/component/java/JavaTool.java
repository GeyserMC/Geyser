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

package org.geysermc.geyser.api.item.custom.v2.component.java;

import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;
import org.geysermc.geyser.api.util.Holders;

import java.util.List;

/**
 * The tool component can be used to mark if the item can destroy blocks when used in creative mode.
 * For non-vanilla items, it is also important to set a default mining speed
 * and all the rules the tool has to ensure proper block breaking.
 * For vanilla-item overrides, Geyser is able to handle this automatically.
 * @since 2.9.3
 */
public interface JavaTool {

    /**
     * A list of rules this tool has. A tool rule consists of a {@link Holders} of block identifiers, and the break speed for those blocks.
     * Doesn't have to be set for vanilla-item overrides.
     *
     * @return a list of rules this tool has
     * @since 2.9.3
     */
    List<@NonNull Rule> rules();

    /**
     * The default mining speed of the tool. This speed is used when no rules match when breaking a block.
     * Defaults to 0.0, and doesn't have to be set for vanilla-item overrides.
     *
     * @return the default mining speed of the tool
     * @since 2.9.3
     */
    @Positive float defaultMiningSpeed();

    /**
     * Whether this item can destroy blocks when trying to break them in
     * creative mode. Defaults to {@code true}.
     *
     * @return whether this item can destroy blocks in creative mode
     * @since 2.9.3
     */
    boolean canDestroyBlocksInCreative();

    /**
     * Creates a builder for the tool component.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(JavaTool.Builder.class);
    }

    /**
     * Creates a tool component, without any rules or a default mining speed. This should only be used for vanilla-item overrides.
     *
     * @param canDestroyBlocksInCreative determines if the item will break blocks in creative mode
     * @return a tool  component
     * @since 2.9.3
     */
    static @NonNull JavaTool of(boolean canDestroyBlocksInCreative) {
        return builder().canDestroyBlocksInCreative(canDestroyBlocksInCreative).build();
    }

    /**
     * Builder for the tool component.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<JavaTool> {

        /**
         * Adds a rule to the tool. Vanilla-item overrides don't need any rules to be set.
         *
         * @param rule the rule to add
         * @see JavaTool#rules()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder rule(@NonNull Rule rule);

        /**
         * Sets the default mining speed of this tool. Vanilla-item overrides don't need a speed set.
         *
         * @param defaultMiningSpeed the default mining speed of this tool
         * @see JavaTool#defaultMiningSpeed()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder defaultMiningSpeed(@Positive float defaultMiningSpeed);

        /**
         * Sets whether this item can destroy blocks when trying to break them in
         * creative mode.
         *
         * @param canDestroyBlocksInCreative determines if the item will break blocks in creative mode
         * @see JavaTool#canDestroyBlocksInCreative()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder canDestroyBlocksInCreative(boolean canDestroyBlocksInCreative);

        /**
         * Creates the tool component.
         *
         * @return the new component
         * @since 2.9.3
         */
        @Override
        JavaTool build();
    }

    /**
     * A rule for a tool. Consists of a {@link Holders} of block identifiers, and a speed to use for those blocks.
     * @since 2.9.3
     */
    interface Rule {

        /**
         * @return the {@link Holders} of block identifiers that this rule is for
         * @since 2.9.3
         */
        @NonNull Holders blocks();

        /**
         * @return the speed to use when mining a block that matches this rule
         * @since 2.9.3
         */
        float speed();

        /**
         * Creates a builder for a tool rule.
         *
         * @return a new builder
         * @since 2.9.3
         */
        static @NonNull Builder builder() {
            return GeyserApi.api().provider(Rule.Builder.class);
        }

        /**
         * Creates a rule consisting of the given blocks and the given speed.
         *
         * @param blocks the {@link Holders} of block identifiers that this rule is for
         * @param speed the speed to use when mining a block that matches this rule
         * @return a tool rule
         * @since 2.9.3
         */
        static @NonNull Rule of(@NonNull Holders blocks, @Positive float speed) {
            return Rule.builder().blocks(blocks).speed(speed).build();
        }

        /**
         * Builder for a tool rule.
         * @since 2.9.3
         */
        interface Builder extends GenericBuilder<Rule> {

            /**
             * Sets the {@link Holders} of block identifiers that this rule is for.
             *
             * @param blocks the {@link Holders} of block identifiers that this rule is for
             * @return this builder
             * @since 2.9.3
             */
            @This
            Builder blocks(@NonNull Holders blocks);

            /**
             * Sets the speed to use when mining a block that matches this rule
             *
             * @param speed the speed to use
             * @return this builder
             * @since 2.9.3
             */
            @This
            Builder speed(@Positive float speed);

            /**
             * Creates the rule.
             *
             * @return the new rule
             * @since 2.9.3
             */
            @Override
            Rule build();
        }
    }
}
