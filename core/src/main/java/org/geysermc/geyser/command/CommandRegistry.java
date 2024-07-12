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
import org.incendo.cloud.Command.Builder;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.geysermc.geyser.command.GeyserCommand.DEFAULT_ROOT_COMMAND;

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

    private static final String GEYSER_ROOT_PERMISSION = "geyser.command";

    protected final GeyserImpl geyser;
    private final CommandManager<GeyserCommandSource> cloud;
    private final boolean applyRootPermission;

    /**
     * Map of Geyser subcommands to their Commands
     */
    private final Map<String, GeyserCommand> commands = new Object2ObjectOpenHashMap<>(13);

    /**
     * Map of Extensions to maps of their subcommands
     */
    private final Map<Extension, Map<String, GeyserCommand>> extensionCommands = new Object2ObjectOpenHashMap<>(0);

    /**
     * Map of root commands (that are for extensions) to Extensions
     */
    private final Map<String, Extension> extensionRootCommands = new Object2ObjectOpenHashMap<>(0);

    /**
     * Map containing only permissions that have been registered with a default value
     */
    protected final Map<String, TriState> permissionDefaults = new Object2ObjectOpenHashMap<>(13);

    /**
     * Creates a new CommandRegistry. Does apply a root permission. If undesired, use the other constructor.
     */
    public CommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        this(geyser, cloud, true);
    }

    /**
     * Creates a new CommandRegistry
     *
     * @param geyser the Geyser instance
     * @param cloud the cloud command manager to register commands to
     * @param applyRootPermission true if this registry should apply a permission to Geyser and Extension root commands.
     *                            This currently exists because we want to retain the root command permission for Spigot,
     *                            but don't want to add it yet to platforms like Velocity where we cannot natively
     *                            specify a permission default. Doing so will break setups as players would suddenly not
     *                            have the required permission to execute any Geyser commands.
     */
    public CommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud, boolean applyRootPermission) {
        this.geyser = geyser;
        this.cloud = cloud;
        this.applyRootPermission = applyRootPermission;

        // register our custom exception handlers
        ExceptionHandlers.register(cloud);

        // begin command registration
        HelpCommand help = new HelpCommand(DEFAULT_ROOT_COMMAND, "help", "geyser.commands.help.desc", "geyser.command.help", this.commands);
        registerBuiltInCommand(help);
        buildRootCommand(GEYSER_ROOT_PERMISSION, help); // build root and delegate to help

        registerBuiltInCommand(new ListCommand(geyser, "list", "geyser.commands.list.desc", "geyser.command.list"));
        registerBuiltInCommand(new ReloadCommand(geyser, "reload", "geyser.commands.reload.desc", "geyser.command.reload"));
        registerBuiltInCommand(new OffhandCommand("offhand", "geyser.commands.offhand.desc", "geyser.command.offhand"));
        registerBuiltInCommand(new DumpCommand(geyser, "dump", "geyser.commands.dump.desc", "geyser.command.dump"));
        registerBuiltInCommand(new VersionCommand(geyser, "version", "geyser.commands.version.desc", "geyser.command.version"));
        registerBuiltInCommand(new SettingsCommand("settings", "geyser.commands.settings.desc", "geyser.command.settings"));
        registerBuiltInCommand(new StatisticsCommand("statistics", "geyser.commands.statistics.desc", "geyser.command.statistics"));
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

        // Stuff that needs to be done on a per-extension basis
        for (Map.Entry<Extension, Map<String, GeyserCommand>> entry : this.extensionCommands.entrySet()) {
            Extension extension = entry.getKey();

            // Register this extension's root command
            extensionRootCommands.put(extension.rootCommand(), extension);

            // Register help commands for all extensions with commands
            String id = extension.description().id();
            HelpCommand extensionHelp = new HelpCommand(
                extension.rootCommand(),
                "help",
                "geyser.commands.exthelp.desc",
                "geyser.command.exthelp." + id,
                entry.getValue()); // commands it provides help for

            registerExtensionCommand(extension, extensionHelp);
            buildRootCommand("geyser.extension." + id + ".command", extensionHelp);
        }

        // Wait for the right moment (depends on the platform) to register permissions.
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

    protected void register(GeyserCommand command, Map<String, GeyserCommand> commands) {
        String root = command.rootCommand();
        String name = command.name();
        if (commands.containsKey(name)) {
            throw new IllegalArgumentException("Command with root=%s, name=%s already registered".formatted(root, name));
        }

        command.register(cloud);
        commands.put(name, command);
        geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.commands.registered", root + " " + name));

        for (String alias : command.aliases()) {
            commands.put(alias, command);
        }

        String permission = command.permission();
        TriState defaultValue = command.permissionDefault();
        if (!permission.isBlank() && defaultValue != null) {

            TriState existingDefault = permissionDefaults.get(permission);
            // Extensions might be using the same permission for two different commands
            if (existingDefault != null && existingDefault != defaultValue) {
                geyser.getLogger().debug("Overriding permission default %s:%s with %s".formatted(permission, existingDefault, defaultValue));
            }

            permissionDefaults.put(permission, defaultValue);
        }
    }

    /**
     * Registers a root command to cloud that delegates to the given help command.
     * The name of this root command is the root of the given help command.
     *
     * @param permission the permission of the root command. currently, it may or may not be
     *                   applied depending on the platform. see below.
     * @param help the help command to delegate to
     */
    private void buildRootCommand(String permission, HelpCommand help) {
        Builder<GeyserCommandSource> builder = cloud.commandBuilder(help.rootCommand());

        if (applyRootPermission) {
            builder = builder.permission(permission);
            permissionDefaults.put(permission, TriState.TRUE);
        }

        cloud.command(builder.handler(context -> {
            GeyserCommandSource source = context.sender();
            if (!source.hasPermission(help.permission())) {
                // delegate if possible - otherwise we have nothing else to offer the user.
                source.sendLocaleString(ExceptionHandlers.PERMISSION_FAIL_LANG_KEY);
                return;
            }
            help.execute(source);
        }));
    }

    protected void onRegisterPermissions(GeyserRegisterPermissionsEvent event) {
        for (Map.Entry<String, TriState> permission : permissionDefaults.entrySet()) {
            event.register(permission.getKey(), permission.getValue());
        }
    }

    public boolean hasPermission(GeyserCommandSource source, String permission) {
        // Handle blank permissions ourselves, as cloud only handles empty ones
        return permission.isBlank() || cloud.hasPermission(source, permission);
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
        if (command.equals(DEFAULT_ROOT_COMMAND)) {
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
