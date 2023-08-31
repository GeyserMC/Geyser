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

import cloud.commandframework.CommandManager;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.exceptions.NoSuchCommandException;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.command.defaults.AdvancedTooltipsCommand;
import org.geysermc.geyser.command.defaults.AdvancementsCommand;
import org.geysermc.geyser.command.defaults.ConnectionTestCommand;
import org.geysermc.geyser.command.defaults.DumpCommand;
import org.geysermc.geyser.command.defaults.ExtensionsCommand;
import org.geysermc.geyser.command.defaults.HelpCommand;
import org.geysermc.geyser.command.defaults.ListCommand;
import org.geysermc.geyser.command.defaults.OffhandCommand;
import org.geysermc.geyser.command.defaults.ReloadCommand;
import org.geysermc.geyser.command.defaults.SettingsCommand;
import org.geysermc.geyser.command.defaults.StatisticsCommand;
import org.geysermc.geyser.command.defaults.StopCommand;
import org.geysermc.geyser.command.defaults.VersionCommand;
import org.geysermc.geyser.event.type.GeyserDefineCommandsEventImpl;
import org.geysermc.geyser.extension.command.GeyserExtensionCommand;
import org.geysermc.geyser.text.GeyserLocale;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

public final class CommandRegistry {

    private final Map<String, Command> commands = new Object2ObjectOpenHashMap<>(13);
    private final Map<Extension, Map<String, Command>> extensionCommands = new Object2ObjectOpenHashMap<>(0);

    private final GeyserImpl geyser;
    private final CommandManager<GeyserCommandSource> cloud;

    public CommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        this.geyser = geyser;
        this.cloud = cloud;

        cloud.registerCommandPostProcessor(new SenderTypeProcessor());

        registerBuiltInCommand(new HelpCommand(geyser, "help", "geyser.commands.help.desc", "geyser.command.help", "geyser", this.commands));
        registerBuiltInCommand(new ListCommand(geyser, "list", "geyser.commands.list.desc", "geyser.command.list"));
        registerBuiltInCommand(new ReloadCommand(geyser, "reload", "geyser.commands.reload.desc", "geyser.command.reload"));
        registerBuiltInCommand(new OffhandCommand(geyser, "offhand", "geyser.commands.offhand.desc", "geyser.command.offhand"));
        registerBuiltInCommand(new DumpCommand(geyser, "dump", "geyser.commands.dump.desc", "geyser.command.dump"));
        registerBuiltInCommand(new VersionCommand(geyser, "version", "geyser.commands.version.desc", "geyser.command.version"));
        registerBuiltInCommand(new SettingsCommand(geyser, "settings", "geyser.commands.settings.desc", "geyser.command.settings"));
        registerBuiltInCommand(new StatisticsCommand(geyser, "statistics", "geyser.commands.statistics.desc", "geyser.command.statistics"));
        registerBuiltInCommand(new AdvancementsCommand("advancements", "geyser.commands.advancements.desc", "geyser.command.advancements"));
        registerBuiltInCommand(new AdvancedTooltipsCommand("tooltips", "geyser.commands.advancedtooltips.desc", "geyser.command.tooltips"));
        registerBuiltInCommand(new ConnectionTestCommand(geyser, "connectiontest", "geyser.commands.connectiontest.desc", "geyser.command.connectiontest"));
        if (this.geyser.getPlatformType() == PlatformType.STANDALONE) {
            registerBuiltInCommand(new StopCommand(geyser, "stop", "geyser.commands.stop.desc", "geyser.command.stop"));
        }

        if (this.geyser.extensionManager().extensions().size() > 0) {
            registerBuiltInCommand(new ExtensionsCommand(this.geyser, "extensions", "geyser.commands.extensions.desc", "geyser.command.extensions"));
        }

        GeyserDefineCommandsEvent defineCommandsEvent = new GeyserDefineCommandsEventImpl(this.commands) {

            @Override
            public void register(@NonNull Command command) {
                if (!(command instanceof GeyserExtensionCommand extensionCommand)) {
                    throw new IllegalArgumentException("Expected GeyserExtensionCommand as part of command registration but got " + command + "! Did you use the Command builder properly?");
                }

                registerExtensionCommand(extensionCommand.extension(), extensionCommand);
            }
        };

        this.geyser.eventBus().fire(defineCommandsEvent);

        // Register help commands for all extensions with commands
        for (Map.Entry<Extension, Map<String, Command>> entry : this.extensionCommands.entrySet()) {
            String id = entry.getKey().description().id();
            registerExtensionCommand(entry.getKey(), new HelpCommand(this.geyser, "help", "geyser.commands.exthelp.desc", "geyser.command.exthelp." + id, id, entry.getValue()));
        }
    }

    /**
     * For internal Geyser commands
     */
    public void registerBuiltInCommand(GeyserCommand command) {
        register(command, this.commands);
    }

    public void registerExtensionCommand(@NonNull Extension extension, @NonNull GeyserCommand command) {
        register(command, this.extensionCommands.computeIfAbsent(extension, e -> new HashMap<>()));
    }

    private void register(GeyserCommand command, Map<String, Command> commands) {
        command.register(cloud);

        commands.put(command.name(), command);
        geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.commands.registered", command.name()));

        if (command.aliases().isEmpty()) {
            return;
        }

        for (String alias : command.aliases()) {
            commands.put(alias, command);
        }
    }

    public void clear() {
        this.commands.clear();
        this.extensionCommands.clear();
    }

    @NotNull
    public Map<String, Command> commands() {
        return Collections.unmodifiableMap(this.commands);
    }

    @NotNull
    public Map<Extension, Map<String, Command>> extensionCommands() {
        return Collections.unmodifiableMap(this.extensionCommands);
    }

    @NotNull
    public CommandManager<GeyserCommandSource> cloud() {
        return cloud;
    }

    /**
     * Returns the description of the given command
     *
     * @param command Command to get the description for
     * @return Command description
     */
    public String description(String command) {
        return ""; // todo: reimplement
    }

    /**
     * Dispatches a command into cloud and handles any thrown exceptions.
     */
    public void runCommand(@NonNull GeyserCommandSource source, @NonNull String command) {
        cloud.executeCommand(source, command).whenComplete((result, throwable) -> {
            if (throwable == null) {
                return;
            }

            if (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }

            handleThrowable(source, throwable);
        });
    }

    // todo: full localization
    private void handleThrowable(@NonNull GeyserCommandSource source, @NonNull Throwable throwable) {
        // This is modelled after the command executors of each cloud minecraft implementation.
        if (throwable instanceof InvalidSyntaxException syntaxException) {
            cloud.handleException(
                    source,
                    InvalidSyntaxException.class,
                    syntaxException,
                    ($, e) -> source.sendMessage("Invalid Command Syntax. Correct syntax is: " + e.getCorrectSyntax())
            );
        } else if (throwable instanceof InvalidCommandSenderException invalidSenderException) {
            cloud.handleException(
                    source,
                    InvalidCommandSenderException.class,
                    invalidSenderException,
                    ($, e) -> source.sendMessage(throwable.getMessage())
            );
        } else if (throwable instanceof NoPermissionException noPermissionException) {
            cloud.handleException(
                    source,
                    NoPermissionException.class,
                    noPermissionException,
                    ($, e) -> source.sendLocaleString("geyser.bootstrap.command.permission_fail")

            );
        } else if (throwable instanceof NoSuchCommandException noCommandException) {
            cloud.handleException(
                    source,
                    NoSuchCommandException.class,
                    noCommandException,
                    ($, e) -> source.sendLocaleString("geyser.bootstrap.command.not_found")
            );
        } else if (throwable instanceof ArgumentParseException argumentParseException) {
            cloud.handleException(
                    source,
                    ArgumentParseException.class,
                    argumentParseException,
                    ($, e) -> source.sendMessage("Invalid Command Argument: " + throwable.getCause().getMessage())
            );
        } else if (throwable instanceof CommandExecutionException executionException) {
            cloud.handleException(
                    source,
                    CommandExecutionException.class,
                    executionException,
                    ($, e) -> defaultHandler(source, throwable.getCause())
            );
        } else {
            defaultHandler(source, throwable);
        }
    }

    private void defaultHandler(GeyserCommandSource source, Throwable throwable) {
        source.sendLocaleString("command.failed"); // java edition translation key
        GeyserImpl.getInstance().getLogger().error("Exception while executing command handler", throwable);
    }
}
