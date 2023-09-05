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

package org.geysermc.geyser.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.util.TriState;
import org.jetbrains.annotations.Contract;

import java.util.Collections;
import java.util.List;

public abstract class GeyserCommand implements org.geysermc.geyser.api.command.Command {

    /**
     * CommandMeta to indicate that the command is only available to bedrock players. default of false.
     */
    public static final CommandMeta.Key<Boolean> BEDROCK_ONLY = CommandMeta.Key.of(Boolean.class, "bedrock-only", meta -> false);

    /**
     * CommandMeta to indicate that the command is only available to players. default of false.
     */
    public static final CommandMeta.Key<Boolean> PLAYER_ONLY = CommandMeta.Key.of(Boolean.class, "player-only", meta -> false);

    /**
     * The second literal of the command. Note: the first literal is {@link #rootCommand()}.
     */
    @NonNull
    private final String name;

    /**
     * The description of the command - will attempt to be translated.
     */
    @NonNull
    private final String description;

    /**
     * The permission node required to run the command, or blank if not required.
     */
    @NonNull
    private final String permission;

    /**
     * The default value of the permission node.
     * A null value indicates that the permission node should not be registered whatsoever.
     */
    @Nullable
    private final TriState permissionDefault;

    /**
     * True if the command can be executed by non players
     */
    private final boolean executableOnConsole;

    /**
     * True if the command can only be run by bedrock players
     */
    private final boolean bedrockOnly;

    /**
     * The aliases of the command {@link #name}
     */
    protected List<String> aliases = Collections.emptyList();

    public GeyserCommand(@NonNull String name, @Nullable String description,
                         @Nullable String permission, @Nullable TriState permissionDefault,
                         boolean executableOnConsole, boolean bedrockOnly) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Command cannot be null or blank!");
        }

        this.name = name;
        this.description = description != null ? description : "";
        this.permission = permission != null ? permission : "";
        this.permissionDefault = permissionDefault;

        if (bedrockOnly && executableOnConsole) {
            throw new IllegalArgumentException("Command cannot be both bedrockOnly and executableOnConsole");
        }

        this.executableOnConsole = executableOnConsole;
        this.bedrockOnly = bedrockOnly;
    }

    public GeyserCommand(@NonNull String name, @NonNull String description, @Nullable String permission, @NonNull TriState permissionDefault) {
        this(name, description, permission, permissionDefault, true, false);
    }

    @NonNull
    @Override
    public final String name() {
        return name;
    }

    @NonNull
    @Override
    public final String description() {
        return description;
    }

    @NonNull
    @Override
    public final String permission() {
        return permission;
    }

    @Nullable
    public final TriState permissionDefault() {
        return permissionDefault;
    }

    @Override
    public final boolean isExecutableOnConsole() {
        return executableOnConsole;
    }

    @Override
    public final boolean isBedrockOnly() {
        return bedrockOnly;
    }

    @NonNull
    @Override
    public final List<String> aliases() {
        return Collections.unmodifiableList(aliases);
    }

    /**
     * @return the first (literal) argument of this command, which comes before {@link #name()}.
     */
    public String rootCommand() {
        return "geyser";
    }

    /**
     * Creates a new command builder with {@link #rootCommand()}, {@link #name()}, and {@link #aliases()} built on it.
     * The Applicable from {@link #meta()} is also applied to the builder.
     */
    @Contract(value = "_ -> new", pure = true)
    public final Command.Builder<GeyserCommandSource> baseBuilder(CommandManager<GeyserCommandSource> manager) {
        return manager.commandBuilder(rootCommand())
            .literal(name, aliases.toArray(new String[0]))
            .permission(permission)
            .apply(meta());
    }

    /**
     * @return an Applicable that applies {@link #BEDROCK_ONLY} and {@link #PLAYER_ONLY} as meta,
     * according to {@link #isBedrockOnly()} and {@link #isExecutableOnConsole()} (respectively).
     */
    public Command.Builder.Applicable<GeyserCommandSource> meta() {
        return builder -> builder
            .meta(BEDROCK_ONLY, isBedrockOnly())
            .meta(PLAYER_ONLY, !isExecutableOnConsole());
    }

    /**
     * Registers this command to the given command manager.
     * This method may be overridden to register more than one command.
     * <br><br>
     * The default implementation is that {@link #baseBuilder(CommandManager)} with {@link #execute(CommandContext)}
     * applied as the handler is registered to the manager.
     */
    public void register(CommandManager<GeyserCommandSource> manager) {
        manager.command(baseBuilder(manager).handler(this::execute));
    }

    /**
     * Executes this command
     * @param context the context with which this command should be executed
     */
    public abstract void execute(CommandContext<GeyserCommandSource> context);
}