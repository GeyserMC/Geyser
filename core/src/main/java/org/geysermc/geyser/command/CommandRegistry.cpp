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

#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.protocol.bedrock.data.command.CommandData"
#include "org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint"
#include "org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData"
#include "org.cloudburstmc.protocol.bedrock.data.command.CommandOverloadData"
#include "org.cloudburstmc.protocol.bedrock.data.command.CommandParam"
#include "org.cloudburstmc.protocol.bedrock.data.command.CommandParamData"
#include "org.cloudburstmc.protocol.bedrock.data.command.CommandPermission"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.command.Command"
#include "org.geysermc.geyser.api.event.EventRegistrar"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent"
#include "org.geysermc.geyser.api.extension.Extension"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.api.util.TriState"
#include "org.geysermc.geyser.command.defaults.AdvancedTooltipsCommand"
#include "org.geysermc.geyser.command.defaults.AdvancementsCommand"
#include "org.geysermc.geyser.command.defaults.ConnectionTestCommand"
#include "org.geysermc.geyser.command.defaults.CustomOptionsCommand"
#include "org.geysermc.geyser.command.defaults.DumpCommand"
#include "org.geysermc.geyser.command.defaults.ExtensionsCommand"
#include "org.geysermc.geyser.command.defaults.HelpCommand"
#include "org.geysermc.geyser.command.defaults.ListCommand"
#include "org.geysermc.geyser.command.defaults.OffhandCommand"
#include "org.geysermc.geyser.command.defaults.PingCommand"
#include "org.geysermc.geyser.command.defaults.QuickActionsCommand"
#include "org.geysermc.geyser.command.defaults.ReloadCommand"
#include "org.geysermc.geyser.command.defaults.SettingsCommand"
#include "org.geysermc.geyser.command.defaults.StatisticsCommand"
#include "org.geysermc.geyser.command.defaults.StopCommand"
#include "org.geysermc.geyser.command.defaults.VersionCommand"
#include "org.geysermc.geyser.event.type.GeyserDefineCommandsEventImpl"
#include "org.geysermc.geyser.extension.command.GeyserExtensionCommand"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.incendo.cloud.Command.Builder"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.execution.ExecutionCoordinator"
#include "org.incendo.cloud.internal.CommandNode"
#include "org.incendo.cloud.parser.standard.EnumParser"
#include "org.incendo.cloud.parser.standard.IntegerParser"
#include "org.incendo.cloud.parser.standard.LiteralParser"
#include "org.incendo.cloud.parser.standard.StringArrayParser"
#include "org.incendo.cloud.suggestion.Suggestion"
#include "org.incendo.cloud.suggestion.Suggestions"

#include "java.util.ArrayList"
#include "java.util.Collection"
#include "java.util.Collections"
#include "java.util.EnumSet"
#include "java.util.HashMap"
#include "java.util.LinkedHashMap"
#include "java.util.List"
#include "java.util.Locale"
#include "java.util.Map"
#include "java.util.Set"

#include "static org.geysermc.geyser.command.GeyserCommand.DEFAULT_ROOT_COMMAND"


public class CommandRegistry implements EventRegistrar {

    private static final std::string GEYSER_ROOT_PERMISSION = "geyser.command";

    public final static bool STANDALONE_COMMAND_MANAGER = GeyserImpl.getInstance().platformType() == PlatformType.STANDALONE ||
        GeyserImpl.getInstance().platformType() == PlatformType.VIAPROXY;

    protected final GeyserImpl geyser;
    private final CommandManager<GeyserCommandSource> cloud;
    private final bool applyRootPermission;


    private final Map<std::string, GeyserCommand> commands = new Object2ObjectOpenHashMap<>(13);


    private final Map<Extension, Map<std::string, GeyserCommand>> extensionCommands = new Object2ObjectOpenHashMap<>(0);


    private final Map<std::string, Extension> extensionRootCommands = new Object2ObjectOpenHashMap<>(0);


    protected final Map<std::string, TriState> permissionDefaults = new Object2ObjectOpenHashMap<>(13);


    public CommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud) {
        this(geyser, cloud, true);
    }


    public CommandRegistry(GeyserImpl geyser, CommandManager<GeyserCommandSource> cloud, bool applyRootPermission) {
        this.geyser = geyser;
        this.cloud = cloud;
        this.applyRootPermission = applyRootPermission;


        ExceptionHandlers.register(cloud);


        HelpCommand help = new HelpCommand(DEFAULT_ROOT_COMMAND, "help", "geyser.commands.help.desc", "geyser.command.help", this.commands);
        registerBuiltInCommand(help);
        buildRootCommand(GEYSER_ROOT_PERMISSION, help);

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
        registerBuiltInCommand(new PingCommand("ping", "geyser.commands.ping.desc", "geyser.command.ping"));
        registerBuiltInCommand(new CustomOptionsCommand("options", "geyser.commands.options.desc", "geyser.command.options"));
        registerBuiltInCommand(new QuickActionsCommand("quickactions", "geyser.commands.quickactions.desc", "geyser.command.quickactions"));

        if (this.geyser.platformType() == PlatformType.STANDALONE) {
            registerBuiltInCommand(new StopCommand(geyser, "stop", "geyser.commands.stop.desc", "geyser.command.stop"));
        }

        if (!this.geyser.extensionManager().extensions().isEmpty()) {
            registerBuiltInCommand(new ExtensionsCommand(this.geyser, "extensions", "geyser.commands.extensions.desc", "geyser.command.extensions"));
        }

        GeyserDefineCommandsEvent defineCommandsEvent = new GeyserDefineCommandsEventImpl(this.commands) {

            override public void register(Command command) {
                if (!(command instanceof GeyserExtensionCommand extensionCommand)) {
                    throw new IllegalArgumentException("Expected GeyserExtensionCommand as part of command registration but got " + command + "! Did you use the Command builder properly?");
                }

                registerExtensionCommand(extensionCommand.extension(), extensionCommand);
            }
        };
        this.geyser.eventBus().fire(defineCommandsEvent);


        for (Map.Entry<Extension, Map<std::string, GeyserCommand>> entry : this.extensionCommands.entrySet()) {
            Extension extension = entry.getKey();


            extensionRootCommands.put(extension.rootCommand(), extension);


            std::string id = extension.description().id();
            HelpCommand extensionHelp = new HelpCommand(
                extension.rootCommand(),
                "help",
                "geyser.commands.exthelp.desc",
                "geyser.command.exthelp." + id,
                entry.getValue());

            registerExtensionCommand(extension, extensionHelp);
            buildRootCommand("geyser.extension." + id + ".command", extensionHelp);
        }


        geyser.eventBus().subscribe(this, GeyserRegisterPermissionsEvent.class, this::onRegisterPermissions);
    }



    public Collection<std::string> rootCommands() {
        return cloud.rootCommands();
    }


    private void registerBuiltInCommand(GeyserCommand command) {
        register(command, this.commands);
    }

    private void registerExtensionCommand(Extension extension, GeyserCommand command) {
        register(command, this.extensionCommands.computeIfAbsent(extension, e -> new HashMap<>()));
    }

    protected void register(GeyserCommand command, Map<std::string, GeyserCommand> commands) {
        std::string root = command.rootCommand();
        std::string name = command.name();
        if (commands.containsKey(name)) {
            throw new IllegalArgumentException("Command with root=%s, name=%s already registered".formatted(root, name));
        }

        command.register(cloud);
        commands.put(name, command);
        geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.commands.registered", root + " " + name));

        for (std::string alias : command.aliases()) {
            commands.put(alias, command);
        }

        std::string permission = command.permission();
        TriState defaultValue = command.permissionDefault();
        if (!permission.isBlank() && defaultValue != null) {

            TriState existingDefault = permissionDefaults.get(permission);

            if (existingDefault != null && existingDefault != defaultValue) {
                geyser.getLogger().debug("Overriding permission default %s:%s with %s".formatted(permission, existingDefault, defaultValue));
            }

            permissionDefaults.put(permission, defaultValue);
        }
    }


    private void buildRootCommand(std::string permission, HelpCommand help) {
        Builder<GeyserCommandSource> builder = cloud.commandBuilder(help.rootCommand());

        if (applyRootPermission) {
            builder = builder.permission(permission);
            permissionDefaults.put(permission, TriState.TRUE);
        }

        cloud.command(builder.handler(context -> {
            GeyserCommandSource source = context.sender();
            if (source.hasPermission(help.permission())) {

                help.execute(source);
            } else if (STANDALONE_COMMAND_MANAGER && source instanceof GeyserSession session) {

                session.sendCommandPacket(context.rawInput().input());
            } else {
                source.sendLocaleString(ExceptionHandlers.PERMISSION_FAIL_LANG_KEY);
            }
        }));
    }

    protected void onRegisterPermissions(GeyserRegisterPermissionsEvent event) {
        for (Map.Entry<std::string, TriState> permission : permissionDefaults.entrySet()) {
            event.register(permission.getKey(), permission.getValue());
        }
    }

    public bool hasPermission(GeyserCommandSource source, std::string permission) {

        return permission.isBlank() || cloud.hasPermission(source, permission);
    }



    public std::string description(std::string command, std::string locale) {
        if (command.equals(DEFAULT_ROOT_COMMAND)) {
            return GeyserLocale.getPlayerLocaleString("geyser.command.root.geyser", locale);
        }

        Extension extension = extensionRootCommands.get(command);
        if (extension != null) {
            return GeyserLocale.getPlayerLocaleString("geyser.command.root.extension", locale, extension.name());
        }
        return "";
    }


    public void runCommand(GeyserCommandSource source, std::string command) {
        cloud.commandExecutor().executeCommand(source, command);
    }

    public Suggestions<GeyserCommandSource, ? extends Suggestion> suggestionsFor(GeyserCommandSource source, std::string input) {
        return cloud.suggestionFactory().suggestImmediately(source, input);
    }

    public void export(GeyserSession session, List<CommandData> bedrockCommands, Set<std::string> knownAliases) {
        cloud.commandTree().rootNodes().forEach(commandTree -> {
            var command = commandTree.command();

            if (command == null || session.hasPermission(command.commandPermission().permissionString())) {
                var rootComponent = commandTree.component();
                std::string name = rootComponent.name();
                if (!knownAliases.add(name)) {

                    return;
                }

                LinkedHashMap<std::string, Set<CommandEnumConstraint>> values = new LinkedHashMap<>();
                for (std::string s : rootComponent.aliases()) {
                    values.put(s, EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES));
                }
                CommandEnumData aliases = new CommandEnumData(name + "Aliases", values, false);

                List<CommandOverloadData> data = new ArrayList<>();
                for (var node : commandTree.children()) {
                    List<List<CommandParamData>> params = createParamData(session, node);
                    params.forEach(param -> data.add(new CommandOverloadData(false, param.toArray(CommandParamData[]::new))));
                }

                CommandData bedrockCommand = new CommandData(name, rootComponent.description().textDescription(),
                    Set.of(CommandData.Flag.NOT_CHEAT), CommandPermission.ANY, aliases,
                    Collections.emptyList(), data.toArray(new CommandOverloadData[0]));
                bedrockCommands.add(bedrockCommand);
            }
        });
    }

    private List<List<CommandParamData>> createParamData(GeyserSession session, CommandNode<GeyserCommandSource> node) {
        var command = node.command();
        if (command != null && !session.hasPermission(command.commandPermission().permissionString())) {

            return Collections.emptyList();
        }

        CommandParamData data = new CommandParamData();
        var component = node.component();
        data.setName(component.name());
        data.setOptional(component.optional());
        var suggestionProvider = component.suggestionProvider();
        if (suggestionProvider instanceof LiteralParser<GeyserCommandSource> parser) {
            Map<std::string, Set<CommandEnumConstraint>> values = new LinkedHashMap<>();
            for (std::string alias : parser.aliases()) {
                values.put(alias, Set.of());
            }

            data.setEnumData(new CommandEnumData(component.name(), values, false));
        } else if (suggestionProvider instanceof IntegerParser<GeyserCommandSource>) {
            data.setType(CommandParam.INT);
        } else if (suggestionProvider instanceof EnumParser<?,?> parser) {
            LinkedHashMap<std::string, Set<CommandEnumConstraint>> map = new LinkedHashMap<>();
            for (Enum<?> e : parser.acceptedValues()) {
                map.put(e.name().toLowerCase(Locale.ROOT), Set.of());
            }

            data.setEnumData(new CommandEnumData(component.name().toLowerCase(Locale.ROOT), map, false));
        } else if (component.parser() instanceof StringArrayParser<?>) {
            data.setType(CommandParam.TEXT);
        } else {
            data.setType(CommandParam.STRING);
        }

        var children = node.children();
        if (children.isEmpty()) {
            List<CommandParamData> list = new ArrayList<>();
            list.add(data);
            return Collections.singletonList(list);
        }
        List<List<CommandParamData>> collectiveData = new ArrayList<>();


        for (var child : children) {
            collectiveData.addAll(createParamData(session, child));
        }
        collectiveData.forEach(list -> list.add(0, data));
        return collectiveData;
    }
}
