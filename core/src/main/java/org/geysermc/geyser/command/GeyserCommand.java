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
import cloud.commandframework.meta.CommandMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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

    @NonNull
    protected final String name;

    /**
     * The description of the command - will attempt to be translated.
     */
    @NonNull
    protected final String description;

    @NonNull
    protected final String permission;

    protected final boolean executableOnConsole;
    protected final boolean bedrockOnly;

    protected List<String> aliases = Collections.emptyList();

    public GeyserCommand(@NonNull String name, @Nullable String description, @Nullable String permission, boolean executableOnConsole, boolean bedrockOnly) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Command cannot be null or blank!");
        }

        this.name = name;
        this.description = description != null ? description : "";
        this.permission = permission != null ? permission : "";

        if (bedrockOnly && executableOnConsole) {
            throw new IllegalArgumentException("Command cannot be both bedrockOnly and executableOnConsole");
        }

        this.executableOnConsole = executableOnConsole;
        this.bedrockOnly = bedrockOnly;
    }

    public GeyserCommand(@NonNull String name, @NonNull String description, @Nullable String permission) {
        this(name, description, permission, true, false);
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

    public String rootCommand() {
        return "geyser";
    }

    @Contract(value = "_ -> new", pure = true)
    public Command.Builder<GeyserCommandSource> builder(CommandManager<GeyserCommandSource> manager) {
        return manager.commandBuilder(rootCommand())
            .literal(name, aliases.toArray(new String[0]))
            .meta(BEDROCK_ONLY, isBedrockOnly())
            .meta(PLAYER_ONLY, !isExecutableOnConsole())
            .permission(permission);
    }

    public void register(CommandManager<GeyserCommandSource> manager) {
        manager.command(builder(manager));
    }
}