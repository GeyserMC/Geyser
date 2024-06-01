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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.api.util.TriState;
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
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers all built-in and extension commands to the given Cloud CommandManager.
 * <p>
 * Fires {@link GeyserDefineCommandsEvent} upon construction.
 * <p>
 * Subscribes to {@link GeyserRegisterPermissionsEvent} upon construction.
 * A new instance of this class (that registers the same permissions) shouldn't be created until the previous
 * instance is unsubscribed from the event.
 */
public class CommandRegistry implements EventRegistrar {

    private final GeyserImpl geyser;
    private final CommandManager<GeyserCommandSource> cloud;

    /**
     * Map of Geyser subcommands to their Commands
     */
    private final Map<String, Command> commands = new Object2ObjectOpenHashMap<>(13);

    /**
     * Map of Extensions to maps of their subcommands
     */
    private final Map<Extension, Map<String, Command>> extensionCommands = new Object2ObjectOpenHashMap<>(0);

    /**
     * Map of root commands (that are for extensions) to Extensions
     */
    private final Map<String, Extension> extensionRootCommands = new Object2ObjectOpenHashMap<>(0);

    /**
     * Map containing only permissions that have been registered with a default value
     */
    private final Map<String, TriState> permissionDefaults = new Object2ObjectOpenHashMap<>(13);

    public CommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        this.geyser = geyser;
        this.cloud = cloud;

        // register our custom exception handlers
        ExceptionHandlers.register(cloud);

        // begin command registration
        registerBuiltInCommand(new HelpCommand(geyser, "help", "geyser.commands.help.desc", "geyser.command.help", "geyser", "geyser.command", this.commands));
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

        if (!this.geyser.extensionManager().extensions().isEmpty()) {
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

        for (Map.Entry<Extension, Map<String, Command>> entry : this.extensionCommands.entrySet()) {
            Extension extension = entry.getKey();

            // Register this extension's root command
            extensionRootCommands.put(extension.rootCommand(), extension);

            // Register help commands for all extensions with commands
            String id = extension.description().id();
            registerExtensionCommand(extension, new HelpCommand(
                this.geyser,
                "help",
                "geyser.commands.exthelp.desc",
                "geyser.command.exthelp." + id,
                extension.rootCommand(),
                extension.description().id() + ".command",
                entry.getValue()));
        }

        // wait for the right moment (depends on the platform) to register permissions
        geyser.eventBus().subscribe(this, GeyserRegisterPermissionsEvent.class, this::onRegisterPermissions);
    }

    /**
     * @return an immutable view of the root commands registered to this command registry
     */
    @NonNull
    public Collection<String> rootCommands() {
        return cloud.rootCommands();
    }

    /**
     * For internal Geyser commands
     */
    private void registerBuiltInCommand(GeyserCommand command) {
        register(command, this.commands);
    }

    private void registerExtensionCommand(@NonNull Extension extension, @NonNull GeyserCommand command) {
        register(command, this.extensionCommands.computeIfAbsent(extension, e -> new HashMap<>()));
    }

    private void register(GeyserCommand command, Map<String, Command> commands) {
        command.register(cloud);

        commands.put(command.name(), command);
        geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.commands.registered", command.name()));

        for (String alias : command.aliases()) {
            commands.put(alias, command);
        }

        if (!command.permission().isBlank() && command.permissionDefault() != null) {
            permissionDefaults.put(command.permission(), command.permissionDefault());
        }

        if (command instanceof HelpCommand helpCommand) {
            permissionDefaults.put(helpCommand.rootCommand(), helpCommand.permissionDefault());
        }
    }

    private void onRegisterPermissions(GeyserRegisterPermissionsEvent event) {
        for (Map.Entry<String, TriState> permission : permissionDefaults.entrySet()) {
            event.register(permission.getKey(), permission.getValue());
        }

        // Register other various Geyser permissions
        event.register(Constants.UPDATE_PERMISSION, TriState.NOT_SET);
        event.register(Constants.SERVER_SETTINGS_PERMISSION, TriState.NOT_SET);
        event.register(Constants.SETTINGS_GAMERULES_PERMISSION, TriState.NOT_SET);
    }

    public boolean hasPermission(GeyserCommandSource source, String permission) {
        return cloud.hasPermission(source, permission);
    }

    /**
     * Returns the description of the given command
     *
     * @param command the root command node
     * @param locale the ideal locale that the description should be in
     * @return a description if found, otherwise an empty string. The locale is not guaranteed.
     */
    @NonNull
    public String description(@NonNull String command, @NonNull String locale) {
        if (command.equals(GeyserCommand.DEFAULT_ROOT_COMMAND)) {
            return GeyserLocale.getPlayerLocaleString("geyser.command.root.geyser", locale);
        }

        Extension extension = extensionRootCommands.get(command);
        if (extension != null) {
            return GeyserLocale.getPlayerLocaleString("geyser.command.root.extension", locale, extension.name());
        }
        return "";
    }

    /**
     * Dispatches a command into cloud and handles any thrown exceptions.
     * This method may or may not be blocking, depending on the {@link ExecutionCoordinator} in use by cloud.
     */
    public void runCommand(@NonNull GeyserCommandSource source, @NonNull String command) {
        cloud.commandExecutor().executeCommand(source, command);
    }
}
