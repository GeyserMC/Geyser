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

package org.geysermc.geyser.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.TriState;

import java.util.Collections;
import java.util.List;

/**
 * Represents a command.
 */
public interface Command {

    /**
     * Gets the command name.
     *
     * @return the command name
     */
    @NonNull
    String name();

    /**
     * Gets the command description.
     *
     * @return the command description
     */
    @NonNull
    String description();

    /**
     * Gets the permission node associated with
     * this command.
     *
     * @return the permission node for this command if defined, otherwise an empty string
     */
    @NonNull
    String permission();

    /**
     * Gets the aliases for this command, as an unmodifiable list
     *
     * @return the aliases for this command as an unmodifiable list
     */
    @NonNull
    List<String> aliases();

    /**
     * Gets if this command is designed to be used only by server operators.
     *
     * @return if this command is designated to be used only by server operators.
     * @deprecated this method is not guaranteed to provide meaningful or expected results.
     */
    @Deprecated(forRemoval = true)
    default boolean isSuggestedOpOnly() {
        return false;
    }

    /**
     * @return true if this command is executable on console
     * @deprecated use {@link #isPlayerOnly()} instead (inverted)
     */
    @Deprecated(forRemoval = true)
    default boolean isExecutableOnConsole() {
        return !isPlayerOnly();
    }

    /**
     * @return true if this command can only be used by players
     */
    boolean isPlayerOnly();

    /**
     * @return true if this command can only be used by Bedrock players
     */
    boolean isBedrockOnly();

    /**
     * @deprecated this method will always return an empty immutable list
     */
    @Deprecated(forRemoval = true)
    @NonNull
    default List<String> subCommands() {
        return Collections.emptyList();
    }

    /**
     * Creates a new {@link Command.Builder} used to construct commands.
     *
     * @param extension the extension
     * @param <T> the source type
     * @return a new command builder used to construct commands
     */
    static <T extends CommandSource> Command.Builder<T> builder(@NonNull Extension extension) {
        return GeyserApi.api().provider(Builder.class, extension);
    }

    interface Builder<T extends CommandSource> {

        /**
         * Defines the source type to use for this command.
         * <p>
         * Command source types can be anything that extend
         * {@link CommandSource}, such as {@link GeyserConnection}.
         * This will guarantee that the source used in the executor
         * is an instance of this source.
         *
         * @param sourceType the source type
         * @return this builder
         */
        Builder<T> source(@NonNull Class<? extends T> sourceType);

        /**
         * Sets the command name.
         *
         * @param name the command name
         * @return this builder
         */
        Builder<T> name(@NonNull String name);

        /**
         * Sets the command description.
         *
         * @param description the command description
         * @return this builder
         */
        Builder<T> description(@NonNull String description);

        /**
         * Sets the permission node required to run this command. <br>
         * It will not be registered with any permission registries, such as an underlying server,
         * or a permissions Extension (unlike {@link #permission(String, TriState)}).
         *
         * @param permission the permission node
         * @return this builder
         */
        Builder<T> permission(@NonNull String permission);

        /**
         * Sets the permission node and its default value. The usage of the default value is platform dependant
         * and may or may not be used. For example, it may be registered to an underlying server.
         * <p>
         * Extensions may instead listen for {@link GeyserRegisterPermissionsEvent} to register permissions,
         * especially if the same permission is required by multiple commands. Also see this event for TriState meanings.
         *
         * @param permission the permission node
         * @param defaultValue the node's default value
         * @return this builder
         * @deprecated this method is experimental and may be removed in the future
         */
        @Deprecated
        Builder<T> permission(@NonNull String permission, @NonNull TriState defaultValue);

        /**
         * Sets the aliases.
         *
         * @param aliases the aliases
         * @return this builder
         */
        Builder<T> aliases(@NonNull List<String> aliases);

        /**
         * Sets if this command is designed to be used only by server operators.
         *
         * @param suggestedOpOnly if this command is designed to be used only by server operators
         * @return this builder
         * @deprecated this method is not guaranteed to produce meaningful or expected results
         */
        @Deprecated(forRemoval = true)
        Builder<T> suggestedOpOnly(boolean suggestedOpOnly);

        /**
         * Sets if this command is executable on console.
         *
         * @param executableOnConsole if this command is executable on console
         * @return this builder
         * @deprecated use {@link #isPlayerOnly()} instead (inverted)
         */
        @Deprecated(forRemoval = true)
        Builder<T> executableOnConsole(boolean executableOnConsole);

        /**
         * Sets if this command can only be executed by players.
         *
         * @param playerOnly if this command is player only
         * @return this builder
         */
        Builder<T> playerOnly(boolean playerOnly);

        /**
         * Sets if this command can only be executed by bedrock players.
         *
         * @param bedrockOnly if this command is bedrock only
         * @return this builder
         */
        Builder<T> bedrockOnly(boolean bedrockOnly);

        /**
         * Sets the subcommands.
         *
         * @param subCommands the subcommands
         * @return this builder
         * @deprecated this method has no effect
         */
        @Deprecated(forRemoval = true)
        default Builder<T> subCommands(@NonNull List<String> subCommands) {
            return this;
        }

        /**
         * Sets the {@link CommandExecutor} for this command.
         *
         * @param executor the command executor
         * @return this builder
         */
        Builder<T> executor(@NonNull CommandExecutor<T> executor);

        /**
         * Builds the command.
         *
         * @return a new command from this builder
         */
        @NonNull
        Command build();
    }
}
