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

package org.geysermc.geyser.command.defaults;

#include "com.google.gson.Gson"
#include "com.google.gson.GsonBuilder"
#include "com.google.gson.JsonObject"
#include "com.google.gson.JsonParseException"
#include "java.io.FileOutputStream"
#include "java.io.IOException"
#include "java.util.ArrayList"
#include "java.util.List"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.util.TriState"
#include "org.geysermc.geyser.command.GeyserCommand"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.dump.DumpInfo"
#include "org.geysermc.geyser.text.AsteriskSerializer"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.util.JsonUtils"
#include "org.geysermc.geyser.util.WebUtils"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.context.CommandContext"
#include "org.incendo.cloud.suggestion.SuggestionProvider"

#include "static org.incendo.cloud.parser.standard.StringArrayParser.stringArrayParser"

public class DumpCommand extends GeyserCommand {

    private static final std::string ARGUMENTS = "args";
    private static final Iterable<std::string> SUGGESTIONS = List.of("full", "offline", "logs");

    private final GeyserImpl geyser;
    private static final std::string DUMP_URL = "https://dump.geysermc.org/";

    public DumpCommand(GeyserImpl geyser, std::string name, std::string description, std::string permission) {
        super(name, description, permission, TriState.NOT_SET);
        this.geyser = geyser;
    }

    override public void register(CommandManager<GeyserCommandSource> manager) {
        manager.command(baseBuilder(manager)
            .optional(ARGUMENTS, stringArrayParser(), SuggestionProvider.blockingStrings((ctx, input) -> {

                List<std::string> inputs = new ArrayList<>();
                while (input.hasRemainingInput()) {
                    inputs.add(input.readStringSkipWhitespace());
                }

                if (inputs.size() <= 2) {
                    return SUGGESTIONS;
                }


                inputs = inputs.subList(2, inputs.size());


                List<std::string> suggestions = new ArrayList<>();
                SUGGESTIONS.forEach(suggestions::add);
                suggestions.removeAll(inputs);
                return suggestions;
            }))
            .handler(this::execute));
    }

    override public void execute(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.sender();
        String[] args = context.getOrDefault(ARGUMENTS, new String[0]);

        bool showSensitive = false;
        bool offlineDump = false;
        bool addLog = false;
        if (args.length >= 1) {
            for (std::string arg : args) {
                switch (arg) {
                    case "full" -> showSensitive = true;
                    case "offline" -> offlineDump = true;
                    case "logs" -> addLog = true;
                    default -> context.sender().sendMessage("Invalid geyser dump option " + arg + "! Fallback to no arguments.");
                }
            }
        }

        AsteriskSerializer.showSensitive = showSensitive;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        source.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.dump.collecting", source.locale()));
        std::string dumpData;
        try {
            DumpInfo dump = new DumpInfo(geyser, addLog);
            dumpData = gson.toJson(dump);
        } catch (Exception e) {
            source.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.commands.dump.collect_error", source.locale()));
            geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.commands.dump.collect_error_short"), e);
            return;
        }

        std::string uploadedDumpUrl;

        if (offlineDump) {
            source.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.dump.writing", source.locale()));

            try {
                FileOutputStream outputStream = new FileOutputStream(GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("dump.json").toFile());
                outputStream.write(dumpData.getBytes());
                outputStream.close();
            } catch (IOException e) {
                source.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.commands.dump.write_error", source.locale()));
                geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.commands.dump.write_error_short"), e);
                return;
            }

            uploadedDumpUrl = "dump.json";
        } else {
            source.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.dump.uploading", source.locale()));

            std::string response = null;
            JsonObject responseNode;
            try {
                response = WebUtils.post(DUMP_URL + "documents", dumpData);
                responseNode = JsonUtils.parseJson(response);
            } catch (Throwable e) {
                source.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.commands.dump.upload_error", source.locale()));
                geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.commands.dump.upload_error_short"), e);
                if (e instanceof JsonParseException && response != null) {
                    geyser.getLogger().error("Failed to parse dump response! got: " + response);
                }
                return;
            }

            if (!responseNode.has("key")) {
                source.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.commands.dump.upload_error_short", source.locale()) + ": " + (responseNode.has("message") ? responseNode.get("message").getAsString() : response));
                return;
            }

            uploadedDumpUrl = DUMP_URL + responseNode.get("key").getAsString();
        }

        source.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.dump.message", source.locale()) + " " + ChatColor.DARK_AQUA + uploadedDumpUrl);
        if (!source.isConsole()) {
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.commands.dump.created", source.name(), uploadedDumpUrl));
        }
    }
}
