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
import cloud.commandframework.execution.CommandExecutionCoordinator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.extension.Extension;
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
import org.geysermc.geyser.event.GeyserEventRegistrar;
import org.geysermc.geyser.event.type.GeyserDefineCommandsEventImpl;
import org.geysermc.geyser.extension.command.GeyserExtensionCommand;
import org.geysermc.geyser.text.GeyserLocale;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

public class CommandRegistry {

    private final GeyserImpl geyser;
    private final CommandManager<GeyserCommandSource> cloud;

    private final Map<String, Command> commands = new Object2ObjectOpenHashMap<>(13);
    private final Map<Extension, Map<String, Command>> extensionCommands = new Object2ObjectOpenHashMap<>(0);

    private final Map<String, TriState> permissions = new Object2ObjectOpenHashMap<>(13);

    /**
     * The order and behaviour of these exception handlers is designed to mirror the typical cloud implementations.
     * For example: https://github.com/Incendo/cloud/blob/a4cc749b91564af57bb7bba36dd8011b556c2b3a/cloud-minecraft/cloud-fabric/src/main/java/cloud/commandframework/fabric/FabricExecutor.java#L94-L173
     */
    // todo: full localization
    private final List<ExceptionHandler<?>> exceptionHandlers = List.of(
        new ExceptionHandler<>(InvalidSyntaxException.class, (src, e) -> src.sendMessage("Invalid Command Syntax. Correct syntax is: " + e.getCorrectSyntax())),
        new ExceptionHandler<>(InvalidCommandSenderException.class, (src, e) -> src.sendMessage(e.getMessage())),
        new ExceptionHandler<>(NoPermissionException.class, (src, e) -> src.sendLocaleString("geyser.bootstrap.command.permission_fail")),
        new ExceptionHandler<>(NoSuchCommandException.class, (src, e) -> src.sendLocaleString("geyser.bootstrap.command.not_found")),
        new ExceptionHandler<>(ArgumentParseException.class, (src, e) -> src.sendMessage("Invalid Command Argument: " + e.getCause().getMessage())),
        new ExceptionHandler<>(CommandExecutionException.class, (src, e) -> handleUnexpectedThrowable(src, e.getCause()))
    );

    public CommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        this.geyser = geyser;
        this.cloud = cloud;

        // Restricts command source types from executing commands they don't have access to
        cloud.registerCommandPostProcessor(new SourceTypeProcessor());
        // Override the default exception handlers that the typical cloud implementations provide so that we can perform localization.
        // This is kind of meaningless for our Geyser-Standalone implementation since these handlers are the default exception handlers in that case.
        for (ExceptionHandler<?> handler : exceptionHandlers) {
            handler.register(cloud);
        }

        // begin command registration
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

        // wait for the right moment (depends on the platform) to register permissions
        geyser.eventBus().subscribe(new GeyserEventRegistrar(this), GeyserRegisterPermissionsEvent.class, this::onRegisterPermissions);
    }

    @NotNull
    public CommandManager<GeyserCommandSource> cloud() {
        return cloud;
    }

    @NotNull
    public Map<String, Command> commands() {
        return Collections.unmodifiableMap(this.commands);
    }

    @NotNull
    public Map<Extension, Map<String, Command>> extensionCommands() {
        return Collections.unmodifiableMap(this.extensionCommands);
    }

    public void clear() {
        this.commands.clear();
        this.extensionCommands.clear();
        this.permissions.clear();
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

        for (String alias : command.aliases()) {
            commands.put(alias, command);
        }

        if (!command.permission().isBlank() && command.permissionDefault() != null) {
            permissions.put(command.permission(), command.permissionDefault());
        }
    }

    private void onRegisterPermissions(GeyserRegisterPermissionsEvent event) {
        for (Map.Entry<String, TriState> permission : permissions.entrySet()) {
            event.register(permission.getKey(), permission.getValue());
        }
    }

    /**
     * Returns the description of the given command
     *
     * @param command Command to get the description for
     * @return Command description
     */
    public String description(String command) {
        // todo: the commands contained in this registry store their descriptions, so those should be checked
        return ""; // todo: reimplement
    }

    /**
     * Dispatches a command into cloud and handles any thrown exceptions.
     * This method may or may not be blocking, depending on the {@link CommandExecutionCoordinator} in use by cloud.
     */
    public void runCommand(@NonNull GeyserCommandSource source, @NonNull String command) {
        cloud.executeCommand(source, command).whenComplete((result, throwable) -> {
            if (throwable == null) {
                return;
            }

            // mirrors typical cloud implementations
            if (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }

            handleThrowable(source, throwable);
        });
    }

    private void handleThrowable(@NonNull GeyserCommandSource source, @NonNull Throwable throwable) {
        if (throwable instanceof Exception exception) {
            for (ExceptionHandler<?> handler : exceptionHandlers) {
                if (handler.handle(source, exception)) {
                    return;
                }
            }
        }
        handleUnexpectedThrowable(source, throwable);
    }

    private void handleUnexpectedThrowable(GeyserCommandSource source, Throwable throwable) {
        source.sendLocaleString("command.failed"); // java edition translation key
        GeyserImpl.getInstance().getLogger().error("Exception while executing command handler", throwable);
    }

    @AllArgsConstructor
    private class ExceptionHandler<E extends Exception> {

        private final Class<E> type;
        private final BiConsumer<GeyserCommandSource, E> handler;

        @SuppressWarnings("unchecked")
        boolean handle(GeyserCommandSource source, Exception exception) {
            if (type.isInstance(exception)) {
                E e = (E) exception;
                // if cloud has a registered exception handler for this type, use it, otherwise use this handler.
                // we register all the exception handlers to cloud, so it will likely just be cloud invoking this same handler.
                cloud.handleException(source, type, e, handler);
                return true;
            }
            return false;
        }

        void register(CommandManager<GeyserCommandSource> manager) {
            manager.registerExceptionHandler(type, handler);
        }
    }
}
