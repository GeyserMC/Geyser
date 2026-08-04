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

import com.sun.management.HotSpotDiagnosticMXBean;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.debug.SessionDebugOption;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;

import javax.management.MBeanServer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.incendo.cloud.parser.standard.BooleanParser.booleanParser;
import static org.incendo.cloud.parser.standard.EnumParser.enumParser;
import static org.incendo.cloud.parser.standard.LongParser.longParser;

public class DebugCommand extends GeyserCommand {
    private static final boolean EXTRA_DEBUG_COMMANDS = Boolean.getBoolean("Geyser.ExtraDebugCommands");

    private final GeyserImpl geyser;

    public DebugCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission, TriState.NOT_SET, false, false);
        this.geyser = geyser;
    }

    @Override
    public void register(CommandManager<GeyserCommandSource> manager) {
        manager.command(baseBuilder(manager)
            .literal("player")
            .literal("options")
            .required("option", enumParser(SessionDebugOption.class))
            .required("xuid", longParser(0))
            .handler(this::execute));

        manager.command(baseBuilder(manager)
            .literal("logging")
            .required("enabled", booleanParser())
            .handler(this::executeLogging));

        manager.command(baseBuilder(manager)
            .literal("heapdump")
            .optional("live", booleanParser())
            .handler(this::executeHeapDump));

        if (EXTRA_DEBUG_COMMANDS) {
            manager.command(baseBuilder(manager)
                    .literal("packet")
                    .literal("by_id")
                    .required("id", IntegerParser.integerParser(0))
                    .handler(this::executePacketById));

            manager.command(baseBuilder(manager)
                .literal("packet")
                .literal("by_name")
                .required("name", StringParser.quotedStringParser())
                .handler(this::executePacketByName));
        }
    }

    @Override
    public void execute(CommandContext<GeyserCommandSource> context) {
        long xuid = context.get("xuid");
        SessionDebugOption option = context.get("option");

        boolean newVal = this.geyser.getDebugSessionMap().toggleDebugOption(xuid, option);

        GeyserSession session = this.geyser.connectionByXuid(String.valueOf(xuid));
        if (session != null) {
            if (session.getDebugOptions().contains(option)) session.getDebugOptions().remove(option);
            else session.getDebugOptions().add(option);

            if (newVal) {
                context.sender().sendMessage("Applied debug option, the player may need to relog in order for the option to apply.");
            } else {
                context.sender().sendMessage("Remove debug option, the player may need to relog in order for the option to apply.");
            }

            return;
        }

        if (newVal) {
            context.sender().sendMessage("Applied debug option, the option will apply when the player logs on.");
        } else {
            context.sender().sendMessage("Remove debug option, the option will apply when the player logs on.");
        }
    }

    private void executeLogging(CommandContext<GeyserCommandSource> context) {
        boolean enabled = context.get("enabled");

        this.geyser.getLogger().setDebug(enabled);
        for (GeyserSession session : this.geyser.onlineConnections()) {
            session.getUpstream().getSession().setLogging(enabled);
        }
    }

    private void executeHeapDump(CommandContext<GeyserCommandSource> context) {
        try {
            Path path = this.geyser.configDirectory().resolve("debug_heapdump.hprof");
            Files.deleteIfExists(path);
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
            mxBean.dumpHeap(path.toString(), (Boolean) context.optional("live").orElse(true));
        } catch (IOException e) {
            this.geyser.getLogger().error("Failed to create heapdump!", e);
            context.sender().sendMessage("Failed to create heapdump.");
        }
    }

    private void executePacketById(CommandContext<GeyserCommandSource> ctx) {
        int id = ctx.get("id");

        for (int protocolVersion : GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS) {
            BedrockCodec codec = GameProtocol.getBedrockCodec(protocolVersion);
            BedrockPacketDefinition<?> definition = codec.getPacketDefinition(id);
            executePacketByInfo(ctx, definition, codec.getMinecraftVersion());
        }
    }

    private void executePacketByName(CommandContext<GeyserCommandSource> ctx) {
        String name = ctx.get("name");

        Class<? extends BedrockPacket> packetClass;
        try {
            packetClass = (Class<? extends BedrockPacket>) Class.forName("org.cloudburstmc.protocol.bedrock.packet." + name);
        } catch (ClassNotFoundException e) {
            ctx.sender().sendMessage("No packet found with that name.");
            return;
        }

        for (int protocolVersion : GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS) {
            BedrockCodec codec = GameProtocol.getBedrockCodec(protocolVersion);
            BedrockPacketDefinition<?> definition = codec.getPacketDefinition(packetClass);
            executePacketByInfo(ctx, definition, codec.getMinecraftVersion());
        }
    }

    private void executePacketByInfo(CommandContext<GeyserCommandSource> ctx, BedrockPacketDefinition<?> definition, String gameVersion) {
        if (definition == null) {
            ctx.sender().sendMessage("Invalid packet ID provided for %s!".formatted(gameVersion));
            return;
        }

        ctx.sender().sendMessage("Version: %s\nPacket ID: %d\nPacket Name: %s\nSerializer: %s".formatted(
            gameVersion, definition.getId(),
            definition.getFactory().get().getClass().getSimpleName(),
            definition.getSerializer().getClass().getName()
        ));
    }
}
