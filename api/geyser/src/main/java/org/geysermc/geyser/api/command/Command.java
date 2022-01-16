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
     * @return the permission node for this command
     */
    @NonNull
    String permission();

    /**
     * Gets the aliases for this command.
     *
     * @return the aliases for this command
     */
    @NonNull
    List<String> aliases();

    /**
     * Gets if this command is executable on console.
     *
     * @return if this command is executable on console
     */
    boolean isExecutableOnConsole();

    /**
     * Gets the subcommands associated with this
     * command. Mainly used within the Geyser Standalone
     * GUI to know what subcommands are supported.
     *
     * @return the subcommands associated with this command
     */
    @NonNull
    default List<String> subCommands() {
        return Collections.emptyList();
    }

    /**
     * Used to send a deny message to Java players if this command can only be used by Bedrock players.
     *
     * @return true if this command can only be used by Bedrock players.
     */
    default boolean isBedrockOnly() {
        return false;
    }

    static <T extends CommandSource> Command.Builder<T> builder(Class<T> sourceType) {
        return GeyserApi.api().commandManager().provideBuilder(sourceType);
    }

    interface Builder<T extends CommandSource> {

        Builder<T> name(String name);

        Builder<T> description(String description);

        Builder<T> permission(String permission);

        Builder<T> aliases(List<String> aliases);

        Builder<T> executableOnConsole(boolean executableOnConsole);

        Builder<T> subCommands(List<String> subCommands);

        Builder<T> bedrockOnly(boolean bedrockOnly);

        Builder<T> executor(CommandExecutor<T> executor);

        Command build();
    }
}
