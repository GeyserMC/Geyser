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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.debug.SessionDebugOption;
import org.geysermc.geyser.session.GeyserSession;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import javax.management.MBeanServer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.incendo.cloud.parser.standard.BooleanParser.booleanParser;
import static org.incendo.cloud.parser.standard.EnumParser.enumParser;
import static org.incendo.cloud.parser.standard.LongParser.longParser;

public class DebugCommand extends GeyserCommand {
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
        GeyserLogger.get().warning("e");
        boolean enabled = context.get("enabled");
        GeyserLogger.get().setDebug(enabled);

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
            GeyserLogger.get().error("Failed to create heapdump!", e);
            context.sender().sendMessage("Failed to create heapdump.");
        }
    }
}
