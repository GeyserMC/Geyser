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

    
    @NonNull
    private final String name;

    
    @NonNull
    private final String description;

    
    @NonNull
    private final String permission;

    
    @Nullable
    private final TriState permissionDefault;

    
    private final boolean playerOnly;

    
    private final boolean bedrockOnly;

    
    protected List<String> aliases = Collections.emptyList();

    public GeyserCommand(@NonNull String name, @NonNull String description,
                         @NonNull String permission, @Nullable TriState permissionDefault,
                         boolean playerOnly, boolean bedrockOnly) {

        if (name.isBlank()) {
            throw new IllegalArgumentException("Command cannot be null or blank!");
        }
        if (permission.isBlank()) {
            
            
            
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

    
    public String rootCommand() {
        return DEFAULT_ROOT_COMMAND;
    }

    
    public final GeyserPermission commandPermission(CommandManager<GeyserCommandSource> manager) {
        return new GeyserPermission(bedrockOnly, playerOnly, permission, manager);
    }

    
    @Contract(value = "_ -> new", pure = true)
    public final Command.Builder<GeyserCommandSource> baseBuilder(CommandManager<GeyserCommandSource> manager) {
        return manager.commandBuilder(rootCommand())
            .literal(name, aliases.toArray(new String[0]))
            .permission(commandPermission(manager))
            .apply(meta());
    }

    
    protected Command.Builder.Applicable<GeyserCommandSource> meta() {
        return builder -> builder
            .commandDescription(CommandDescription.commandDescription(GeyserLocale.getLocaleStringLog(description))); 
    }

    
    public void register(CommandManager<GeyserCommandSource> manager) {
        manager.command(baseBuilder(manager).handler(this::execute));
    }

    
    public abstract void execute(CommandContext<GeyserCommandSource> context);
}
