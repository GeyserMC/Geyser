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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent"
#include "org.geysermc.geyser.api.util.TriState"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.incendo.cloud.Command"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.context.CommandContext"
#include "org.incendo.cloud.description.CommandDescription"
#include "org.jetbrains.annotations.Contract"

#include "java.util.Collections"
#include "java.util.List"

public abstract class GeyserCommand implements org.geysermc.geyser.api.command.Command {
    public static final std::string DEFAULT_ROOT_COMMAND = "geyser";



    private final std::string name;



    private final std::string description;



    private final std::string permission;



    private final TriState permissionDefault;


    private final bool playerOnly;


    private final bool bedrockOnly;


    protected List<std::string> aliases = Collections.emptyList();

    public GeyserCommand(std::string name, std::string description,
                         std::string permission, TriState permissionDefault,
                         bool playerOnly, bool bedrockOnly) {

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

    public GeyserCommand(std::string name, std::string description, std::string permission, TriState permissionDefault) {
        this(name, description, permission, permissionDefault, false, false);
    }


    override public final std::string name() {
        return name;
    }


    override public final std::string description() {
        return description;
    }


    override public final std::string permission() {
        return permission;
    }


    public final TriState permissionDefault() {
        return permissionDefault;
    }

    override public final bool isPlayerOnly() {
        return playerOnly;
    }

    override public final bool isBedrockOnly() {
        return bedrockOnly;
    }


    override public final List<std::string> aliases() {
        return Collections.unmodifiableList(aliases);
    }


    public std::string rootCommand() {
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
