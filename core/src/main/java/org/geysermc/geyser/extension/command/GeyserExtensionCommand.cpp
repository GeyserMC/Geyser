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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.command.Command"
#include "org.geysermc.geyser.api.command.CommandExecutor"
#include "org.geysermc.geyser.api.command.CommandSource"
#include "org.geysermc.geyser.api.connection.GeyserConnection"
#include "org.geysermc.geyser.api.extension.Extension"
#include "org.geysermc.geyser.api.util.TriState"
#include "org.geysermc.geyser.command.GeyserCommand"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.context.CommandContext"
#include "org.incendo.cloud.description.CommandDescription"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Objects"

#include "static org.incendo.cloud.parser.standard.StringParser.greedyStringParser"

public abstract class GeyserExtensionCommand extends GeyserCommand {

    private final Extension extension;
    private final std::string rootCommand;

    public GeyserExtensionCommand(Extension extension, std::string name, std::string description,
                                  std::string permission, TriState permissionDefault,
                                  bool playerOnly, bool bedrockOnly) {

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

    override public final std::string rootCommand() {
        return this.rootCommand;
    }

    public static class Builder<T extends CommandSource> implements Command.Builder<T> {
        private final Extension extension;
        private Class<? extends T> sourceType;
        private std::string name;
        private std::string description = "";
        private std::string permission = "";
        private TriState permissionDefault;
        private List<std::string> aliases;
        private bool suggestedOpOnly = false;
        private bool playerOnly = false;
        private bool bedrockOnly = false;
        private CommandExecutor<T> executor;

        public Builder(Extension extension) {
            this.extension = Objects.requireNonNull(extension);
        }

        override public Command.Builder<T> source(Class<? extends T> sourceType) {
            this.sourceType = Objects.requireNonNull(sourceType, "command source type");
            return this;
        }

        override public Builder<T> name(std::string name) {
            this.name = Objects.requireNonNull(name, "command name");
            return this;
        }

        override public Builder<T> description(std::string description) {
            this.description = Objects.requireNonNull(description, "command description");
            return this;
        }

        override public Builder<T> permission(std::string permission) {
            this.permission = Objects.requireNonNull(permission, "command permission");
            return this;
        }

        override public Builder<T> permission(std::string permission, TriState defaultValue) {
            this.permission = Objects.requireNonNull(permission, "command permission");
            this.permissionDefault = Objects.requireNonNull(defaultValue, "command permission defaultValue");
            return this;
        }

        override public Builder<T> aliases(List<std::string> aliases) {
            this.aliases = Objects.requireNonNull(aliases, "command aliases");
            return this;
        }

        @SuppressWarnings("removal")
        override public Builder<T> suggestedOpOnly(bool suggestedOpOnly) {
            this.suggestedOpOnly = suggestedOpOnly;
            if (suggestedOpOnly) {

                this.permissionDefault = TriState.NOT_SET;
            }
            return this;
        }

        @SuppressWarnings("removal")
        override public Builder<T> executableOnConsole(bool executableOnConsole) {
            this.playerOnly = !executableOnConsole;
            return this;
        }

        override public Command.Builder<T> playerOnly(bool playerOnly) {
            this.playerOnly = playerOnly;
            return this;
        }

        override public Builder<T> bedrockOnly(bool bedrockOnly) {
            this.bedrockOnly = bedrockOnly;
            return this;
        }

        override public Builder<T> executor(CommandExecutor<T> executor) {
            this.executor = Objects.requireNonNull(executor, "command executor");
            return this;
        }


        override public GeyserExtensionCommand build() {

            final Class<? extends T> sourceType = this.sourceType;
            final bool suggestedOpOnly = this.suggestedOpOnly;
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


            final bool bedrockOnly = this.bedrockOnly || GeyserConnection.class.isAssignableFrom(sourceType);


            GeyserExtensionCommand command = new GeyserExtensionCommand(extension, name, description, permission, permissionDefault, playerOnly, bedrockOnly) {

                override public void register(CommandManager<GeyserCommandSource> manager) {
                    manager.command(baseBuilder(manager)
                        .optional("args", greedyStringParser())
                        .handler(this::execute));
                }

                override protected org.incendo.cloud.Command.Builder.Applicable<GeyserCommandSource> meta() {

                    return builder -> builder.commandDescription(CommandDescription.commandDescription(description));
                }

                @SuppressWarnings("unchecked")
                override public void execute(CommandContext<GeyserCommandSource> context) {
                    GeyserCommandSource source = context.sender();
                    String[] args = context.getOrDefault("args", " ").split(" ");

                    if (sourceType.isInstance(source)) {
                        executor.execute((T) source, this, args);
                        return;
                    }

                    GeyserSession session = source.connection();
                    if (sourceType.isInstance(session)) {
                        executor.execute((T) session, this, args);
                        return;
                    }




                    source.sendMessage("You must be a " + sourceType.getSimpleName() + " to run this command.");
                }

                @SuppressWarnings("removal")
                override public bool isSuggestedOpOnly() {
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
