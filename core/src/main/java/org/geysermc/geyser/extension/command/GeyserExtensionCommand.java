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

package org.geysermc.geyser.extension.command;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandExecutor;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;

public abstract class GeyserExtensionCommand extends GeyserCommand {

    private final Extension extension;
    private final String rootCommand;

    public GeyserExtensionCommand(@NonNull Extension extension, @NonNull String name, @NonNull String description,
                                  @NonNull String permission, @Nullable TriState permissionDefault,
                                  boolean playerOnly, boolean bedrockOnly) {

        super(name, description, permission, permissionDefault, playerOnly, bedrockOnly);
        this.extension = extension;
        this.rootCommand = Objects.requireNonNull(extension.rootCommand());

        if (this.rootCommand.isBlank()) {
            throw new IllegalStateException("rootCommand of extension " + extension.name() + " may not be blank");
        }
    }

    public final Extension extension() {
        return this.extension;
    }

    @Override
    public final String rootCommand() {
        return this.rootCommand;
    }

    public static class Builder<T extends CommandSource> implements Command.Builder<T> {
        @NonNull private final Extension extension;
        @Nullable private Class<? extends T> sourceType;
        @Nullable private String name;
        @NonNull private String description = "";
        @NonNull private String permission = "";
        @Nullable private TriState permissionDefault;
        @Nullable private List<String> aliases;
        private boolean suggestedOpOnly = false; // deprecated for removal
        private boolean playerOnly = false;
        private boolean bedrockOnly = false;
        @Nullable private CommandExecutor<T> executor;

        public Builder(@NonNull Extension extension) {
            this.extension = Objects.requireNonNull(extension);
        }

        @Override
        public Command.Builder<T> source(@NonNull Class<? extends T> sourceType) {
            this.sourceType = Objects.requireNonNull(sourceType, "command source type");
            return this;
        }

        @Override
        public Builder<T> name(@NonNull String name) {
            this.name = Objects.requireNonNull(name, "command name");
            return this;
        }

        @Override
        public Builder<T> description(@NonNull String description) {
            this.description = Objects.requireNonNull(description, "command description");
            return this;
        }

        @Override
        public Builder<T> permission(@NonNull String permission) {
            this.permission = Objects.requireNonNull(permission, "command permission");
            return this;
        }

        @Override
        public Builder<T> permission(@NonNull String permission, @NonNull TriState defaultValue) {
            this.permission = Objects.requireNonNull(permission, "command permission");
            this.permissionDefault = Objects.requireNonNull(defaultValue, "command permission defaultValue");
            return this;
        }

        @Override
        public Builder<T> aliases(@NonNull List<String> aliases) {
            this.aliases = Objects.requireNonNull(aliases, "command aliases");
            return this;
        }

        @SuppressWarnings("removal") // this is our doing
        @Override
        public Builder<T> suggestedOpOnly(boolean suggestedOpOnly) {
            this.suggestedOpOnly = suggestedOpOnly;
            if (suggestedOpOnly) {
                // the most amount of legacy/deprecated behaviour I'm willing to support
                this.permissionDefault = TriState.NOT_SET;
            }
            return this;
        }

        @SuppressWarnings("removal") // this is our doing
        @Override
        public Builder<T> executableOnConsole(boolean executableOnConsole) {
            this.playerOnly = !executableOnConsole;
            return this;
        }

        @Override
        public Command.Builder<T> playerOnly(boolean playerOnly) {
            this.playerOnly = playerOnly;
            return this;
        }

        @Override
        public Builder<T> bedrockOnly(boolean bedrockOnly) {
            this.bedrockOnly = bedrockOnly;
            return this;
        }

        @Override
        public Builder<T> executor(@NonNull CommandExecutor<T> executor) {
            this.executor = Objects.requireNonNull(executor, "command executor");
            return this;
        }

        @NonNull
        @Override
        public GeyserExtensionCommand build() {
            // These are captured in the anonymous lambda below and shouldn't change even if the builder does
            final Class<? extends T> sourceType = this.sourceType;
            final boolean suggestedOpOnly = this.suggestedOpOnly;
            final CommandExecutor<T> executor = this.executor;

            if (name == null) {
                throw new IllegalArgumentException("name was not provided for a command in extension " + extension.name());
            }
            if (sourceType == null) {
                throw new IllegalArgumentException("Source type was not defined for command " + name + " in extension " + extension.name());
            }
            if (executor == null) {
                throw new IllegalArgumentException("Command executor was not defined for command " + name + " in extension " + extension.name());
            }

            // if the source type is a GeyserConnection then it is inherently bedrockOnly
            final boolean bedrockOnly = this.bedrockOnly || GeyserConnection.class.isAssignableFrom(sourceType);
            // a similar check would exist for executableOnConsole, but there is not a logger type exposed in the api

            GeyserExtensionCommand command = new GeyserExtensionCommand(extension, name, description, permission, permissionDefault, playerOnly, bedrockOnly) {

                @Override
                public void register(CommandManager<GeyserCommandSource> manager) {
                    manager.command(baseBuilder(manager)
                        .optional("args", greedyStringParser())
                        .handler(this::execute));
                }

                @SuppressWarnings("unchecked")
                @Override
                public void execute(CommandContext<GeyserCommandSource> context) {
                    GeyserCommandSource source = context.sender();
                    String[] args = context.getOrDefault("args", "").split(" ");

                    if (sourceType.isInstance(source)) {
                        executor.execute((T) source, this, args);
                        return;
                    }

                    @Nullable GeyserSession session = source.connection();
                    if (sourceType.isInstance(session)) {
                        executor.execute((T) session, this, args);
                        return;
                    }

                    // currently, the only subclass of CommandSource exposed in the api is GeyserConnection.
                    // when this command was registered, we enabled bedrockOnly if the sourceType was a GeyserConnection.
                    // as a result, the permission checker should handle that case and this method shouldn't even be reached.
                    source.sendMessage("You must be a " + sourceType.getSimpleName() + " to run this command.");
                }

                @SuppressWarnings("removal") // this is our doing
                @Override
                public boolean isSuggestedOpOnly() {
                    return suggestedOpOnly;
                }
            };

            if (aliases != null) {
                command.aliases = new ArrayList<>(aliases);
            }
            return command;
        }
    }
}
