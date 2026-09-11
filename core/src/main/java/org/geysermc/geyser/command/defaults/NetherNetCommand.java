/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.netty.signalling.admission.NativeAdmissionServerChannel;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.signalling.ProviderClient;
import org.cloudburstmc.netty.signalling.ServerStatus;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.network.bedrock.nethernet.NetherNetServer;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.WardenClaimAdapter;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;

public class NetherNetCommand extends GeyserCommand {
    private static final String ARGUMENTS = "arguments";

    private final GeyserImpl geyser;

    public NetherNetCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission, TriState.NOT_SET);
        this.geyser = geyser;
    }

    @Override
    public void register(CommandManager<GeyserCommandSource> manager) {
        manager.command(baseBuilder(manager).handler(this::execute));
        manager.command(baseBuilder(manager)
            .literal("status")
            .optional(ARGUMENTS, greedyStringParser())
            .handler(this::status));
        manager.command(baseBuilder(manager).literal("claim").handler(this::claim));
        manager.command(baseBuilder(manager).literal("diagnostics").handler(this::diagnostics));
    }

    @Override
    public void execute(CommandContext<GeyserCommandSource> context) {
        context.sender().sendMessage("Usage: " + rootCommand() + " " + name() + " <status | claim | diagnostics>");
    }

    /**
     * Inspect or update the provider's automatic status report
     */
    private void status(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.sender();
        NetherNetServer server = server(source);
        if (server == null) {
            return;
        }

        String[] args = context.<String>optional(ARGUMENTS).map(value -> value.trim().split("\\s+")).orElse(new String[0]);
        try {
            if (args.length == 0) {
                source.sendMessage(GeyserImpl.GSON.toJson(server.serverStatus()));
            } else if (args.length == 1 && args[0].equals("automatic")) {
                server.restoreAutomaticServerStatus();
                source.sendMessage("Automatic Geyser status restored.");
            } else if (args.length == 2 && args[0].equals("set") && args[1].length() <= 4096) {
                String json = new String(Base64.getUrlDecoder().decode(args[1]), StandardCharsets.UTF_8);
                server.setServerStatus(GeyserImpl.GSON.fromJson(json, ServerStatus.class));
                source.sendMessage("Complete provider status queued.");
            } else {
                source.sendMessage("Usage: " + rootCommand() + " " + name() + " status [automatic | set <base64url JSON snapshot>]");
            }
        } catch (RuntimeException invalid) {
            source.sendMessage("Invalid complete server status snapshot.");
        }
    }

    /**
     * Refresh an optional Warden ownership link
     */
    private void claim(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.sender();
        NetherNetServer server = server(source);
        if (server == null) {
            return;
        }

        WardenClaimAdapter claim = server.wardenClaim();
        if (claim == null) {
            source.sendMessage("Provider is not running.");
            return;
        }
        claim.refresh().whenComplete((action, failure) -> source.sendMessage(failure == null
                ? action.map(WardenClaimAdapter.Action::message).orElse("This provider has no available Warden ownership action.")
                : "Ownership action unavailable: " + NetherNetServer.providerFailure(failure)));
    }

    /**
     * Inspect native allocation counters and signed provider readiness
     */
    private void diagnostics(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.sender();
        NetherNetServer server = server(source);
        if (server == null) {
            return;
        }

        ProviderClient client = server.providerClient();
        if (client == null) {
            source.sendMessage("Provider is not running.");
            return;
        }

        if (server.providerChannel() instanceof NativeAdmissionServerChannel nativeChannel && nativeChannel.isActive()) {
            source.sendMessage(GeyserImpl.GSON.toJson(Map.of("nativeCreationAttempts", nativeChannel.creationAttempts(),
                    "admission", nativeChannel.admissionStats(), "native", nativeChannel.nativeStats(), "bind", nativeChannel.localAddress().toString())));
        }

        source.sendMessage(GeyserImpl.GSON.toJson(Map.of("droppedGameOutcomeEvents", server.gameOutcomes().droppedEvents())));
        client.readiness().whenComplete((ready, failure) -> {
            source.sendMessage(failure == null ? ready.toString() : NetherNetServer.providerFailure(failure));
        });
    }

    /**
     * @return the running NetherNet server, or null (after notifying the source) if unavailable to this source
     */
    private @Nullable NetherNetServer server(GeyserCommandSource source) {
        if (!source.isConsole()) {
            source.sendMessage("This operation requires the server console.");
            return null;
        }
        NetherNetServer server = geyser.getNetherNetServer();
        if (server == null) {
            source.sendMessage("NetherNet is not running.");
        }
        return server;
    }
}
