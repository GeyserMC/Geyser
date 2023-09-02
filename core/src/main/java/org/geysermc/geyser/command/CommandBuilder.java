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

package org.geysermc.geyser.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandExecutor;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.extension.command.GeyserExtensionCommand;
import org.geysermc.geyser.session.GeyserSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CommandBuilder<T extends CommandSource> implements Command.Builder<T> {
    private final Extension extension;
    private Class<? extends T> sourceType;
    private String name;
    private String description;
    private String permission;
    private List<String> aliases;
    private boolean suggestedOpOnly = false; // deprecated for removal
    private boolean executableOnConsole = true;
    private boolean bedrockOnly = false;
    private CommandExecutor<T> executor;

    public CommandBuilder(Extension extension) {
        this.extension = Objects.requireNonNull(extension);
    }

    @Override
    public Command.Builder<T> source(@NonNull Class<? extends T> sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    @Override
    public CommandBuilder<T> name(@NonNull String name) {
        this.name = name;
        return this;
    }

    @Override
    public CommandBuilder<T> description(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Override
    public CommandBuilder<T> permission(@Nullable String permission) {
        this.permission = permission;
        return this;
    }

    @Override
    public CommandBuilder<T> aliases(@Nullable List<String> aliases) {
        this.aliases = aliases;
        return this;
    }

    @Override
    public Command.Builder<T> suggestedOpOnly(boolean suggestedOpOnly) {
        this.suggestedOpOnly = suggestedOpOnly;
        return this;
    }

    @Override
    public CommandBuilder<T> executableOnConsole(boolean executableOnConsole) {
        this.executableOnConsole = executableOnConsole;
        return this;
    }

    @Override
    public CommandBuilder<T> bedrockOnly(boolean bedrockOnly) {
        this.bedrockOnly = bedrockOnly;
        return this;
    }

    @Override
    public CommandBuilder<T> executor(@NonNull CommandExecutor<T> executor) {
        this.executor = executor;
        return this;
    }

    @NonNull
    @Override
    public GeyserExtensionCommand build() {
        final Class<? extends T> sourceType = this.sourceType;
        final boolean suggestedOpOnly = this.suggestedOpOnly;
        final CommandExecutor<T> executor = this.executor;

        if (sourceType == null) {
            throw new IllegalArgumentException("Source type was not defined for command " + name + " in extension " + extension.name());
        }
        if (executor == null) {
            throw new IllegalArgumentException("Command executor was not defined for command " + name + " in extension " + extension.name());
        }

        GeyserExtensionCommand command = new GeyserExtensionCommand(extension, name, description, permission, executableOnConsole, bedrockOnly) {

            @SuppressWarnings("unchecked")
            @Override
            public cloud.commandframework.Command.Builder<GeyserCommandSource> builder(CommandManager<GeyserCommandSource> manager) {
                return super.builder(manager)
                        .argument(StringArgument.optional("args", StringArgument.StringMode.GREEDY))
                        .handler(context -> {
                            GeyserCommandSource source = context.getSender();
                            String[] args = context.getOrDefault("args", "").split(" ");

                            if (sourceType.isInstance(source)) {
                                executor.execute((T) source, this, args);
                                return;
                            }

                            GeyserSession session = source.connection().orElse(null);
                            if (sourceType.isInstance(session)) {
                                executor.execute((T) session, this, args);
                                return;
                            }

                            // todo: send sender message instead
                            GeyserImpl.getInstance().getLogger().debug("Ignoring command " + this.name + " due to no suitable sender.");
                        });
            }

            @Override
            public boolean isSuggestedOpOnly() {
                return suggestedOpOnly;
            }
        };

        command.aliases = aliases != null ? new ArrayList<>(aliases) : Collections.emptyList();
        return command;
    }
}
