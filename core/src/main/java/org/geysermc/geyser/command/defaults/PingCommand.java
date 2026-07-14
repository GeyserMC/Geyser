/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.incendo.cloud.context.CommandContext;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PingCommand extends GeyserCommand {

    private static final int FULL_STACK_TIMEOUT_SECONDS = 5;

    public PingCommand(String name, String description, String permission) {
        super(name, description, permission, TriState.TRUE, true, true);
    }

    @Override
    public void execute(CommandContext<GeyserCommandSource> context) {
        GeyserSession session = Objects.requireNonNull(context.sender().connection());

        // The localized line carries the transport RTT: the network round
        // trip between the client and Geyser. A remote Java backend measures
        // this plus its own hop to Geyser.
        session.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.commands.ping.message", session.locale(), session.ping()));

        // Full stack: send a NetworkStackLatency probe and time the client's
        // answer, which runs through its processing loop.
        long start = System.nanoTime();
        AtomicBoolean answered = new AtomicBoolean();
        // Hardcoded English: new locale keys would dirty the languages
        // submodule, which we do not fork (yet); acceptable for a debug
        // command. Ideally these move to locale keys via a languages fork.
        ScheduledFuture<?> timeout = session.scheduleInEventLoop(() -> {
            if (answered.compareAndSet(false, true)) {
                session.sendMessage("Full stack ping: no response from the client after "
                        + FULL_STACK_TIMEOUT_SECONDS + " seconds");
            }
        }, FULL_STACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        session.sendNetworkLatencyStackPacket(System.currentTimeMillis(), true, () -> {
            if (answered.compareAndSet(false, true)) {
                timeout.cancel(false);
                long ms = (System.nanoTime() - start) / 1_000_000;
                session.sendMessage("Full stack ping: " + ms
                        + "ms (round trip to Geyser plus your client's processing time; useful for debugging)");
            }
        });
    }
}
