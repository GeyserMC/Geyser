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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.text.GeyserLocale;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.CommandDescription;
import org.jetbrains.annotations.Contract;

import java.util.Collections;
import java.util.List;

public abstract class GeyserCommand implements org.geysermc.geyser.api.command.Command {
    public static final String DEFAULT_ROOT_COMMAND = "geyser";

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
     * See {@link GeyserRegisterPermissionsEvent#register(String, TriState)} for TriState meanings.
     */
    @Nullable
    private final TriState permissionDefault;

    /**
     * True if this command can be executed by players
     */
    private final boolean playerOnly;

    /**
     * True if this command can only be run by bedrock players
     */
    private final boolean bedrockOnly;

    /**
     * The aliases of the command {@link #name}. This should not be modified after construction.
     */
    protected List<String> aliases = Collections.emptyList();

    public GeyserCommand(@NonNull String name, @NonNull String description,
                         @NonNull String permission, @Nullable TriState permissionDefault,
                         boolean playerOnly, boolean bedrockOnly) {

        if (name.isBlank()) {
            throw new IllegalArgumentException("Command cannot be null or blank!");
        }
        if (permission.isBlank()) {
            // Cloud treats empty permissions as available to everyone, but not blank permissions.
            // When registering commands, we must convert ALL whitespace permissions into empty ones,
            // because we cannot override permission checks that Cloud itself performs
            permission = "";
            permissionDefault = null;
        }

        this.name = name;
        this.description = description;
        this.permission = permission;
        this.permissionDefault = permissionDefault;

        if (bedrockOnly && !playerOnly) {
            throw new IllegalArgumentException("Command cannot be bedrockOnly if it is not playerOnly");
        }

        this.playerOnly = playerOnly;
        this.bedrockOnly = bedrockOnly;
    }

    public GeyserCommand(@NonNull String name, @NonNull String description, @NonNull String permission, @Nullable TriState permissionDefault) {
        this(name, description, permission, permissionDefault, false, false);
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
    public final boolean isPlayerOnly() {
        return playerOnly;
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
        return DEFAULT_ROOT_COMMAND;
    }

    /**
     * Returns a {@link org.incendo.cloud.permission.Permission} that handles {@link #isBedrockOnly()}, {@link #isPlayerOnly()}, and {@link #permission()}.
     *
     * @param manager the manager to be used for permission node checking
     * @return a permission that will properly restrict usage of this command
     */
    public final GeyserPermission commandPermission(CommandManager<GeyserCommandSource> manager) {
        return new GeyserPermission(bedrockOnly, playerOnly, permission, manager);
    }

    /**
     * Creates a new command builder with {@link #rootCommand()}, {@link #name()}, and {@link #aliases()} built on it.
     * A permission predicate that takes into account {@link #permission()}, {@link #isBedrockOnly()}, and {@link #isPlayerOnly()}
     * is applied. The Applicable from {@link #meta()} is also applied to the builder.
     */
    @Contract(value = "_ -> new", pure = true)
    public final Command.Builder<GeyserCommandSource> baseBuilder(CommandManager<GeyserCommandSource> manager) {
        return manager.commandBuilder(rootCommand())
            .literal(name, aliases.toArray(new String[0]))
            .permission(commandPermission(manager))
            .apply(meta());
    }

    /**
     * @return an Applicable that applies this command's description
     */
    protected Command.Builder.Applicable<GeyserCommandSource> meta() {
        return builder -> builder
            .commandDescription(CommandDescription.commandDescription(GeyserLocale.getLocaleStringLog(description))); // used in cloud-bukkit impl
    }

    /**
     * Registers this command to the given command manager.
     * This method may be overridden to register more than one command.
     * <p>
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
