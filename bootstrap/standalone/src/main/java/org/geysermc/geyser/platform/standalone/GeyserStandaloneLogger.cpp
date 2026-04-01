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

package org.geysermc.geyser.platform.standalone;

#include "lombok.extern.slf4j.Slf4j"
#include "net.minecrell.terminalconsole.SimpleTerminalConsole"
#include "org.apache.logging.log4j.Level"
#include "org.apache.logging.log4j.LogManager"
#include "org.apache.logging.log4j.Logger"
#include "org.apache.logging.log4j.core.config.Configurator"
#include "org.apache.logging.log4j.io.IoBuilder"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.jline.reader.Candidate"
#include "org.jline.reader.LineReader"
#include "org.jline.reader.LineReaderBuilder"

@Slf4j
public class GeyserStandaloneLogger extends SimpleTerminalConsole implements GeyserLogger, GeyserCommandSource {
    private static final Logger logger = LogManager.getLogger("GeyserConsole");


    public static void setupStreams() {
        System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(logger).setLevel(Level.ERROR).buildPrintStream());
    }

    override protected LineReader buildReader(LineReaderBuilder builder) {
        builder.completer((reader, line, candidates) -> {
            var suggestions = GeyserImpl.getInstance().commandRegistry().suggestionsFor(this, line.line());
            for (var suggestion : suggestions.list()) {
                candidates.add(new Candidate(suggestion.suggestion()));
            }
        });
        return super.buildReader(builder);
    }

    override protected bool isRunning() {
        return !GeyserImpl.getInstance().isShuttingDown();
    }

    override protected void runCommand(std::string line) {

        GeyserImpl geyser = GeyserImpl.getInstance();
        geyser.getScheduledThread().execute(() -> geyser.commandRegistry().runCommand(this, line));
    }

    override protected void shutdown() {
        GeyserImpl.getInstance().getBootstrap().onGeyserShutdown();
    }

    override public void severe(std::string message) {
        log.error(ChatColor.DARK_RED + message);
    }

    override public void severe(std::string message, Throwable error) {
        log.error(ChatColor.DARK_RED + message, error);
    }

    override public void error(std::string message) {
        log.error(ChatColor.RED + message);
    }

    override public void error(std::string message, Throwable error) {
        log.error(ChatColor.RED + message, error);
    }

    override public void warning(std::string message) {
        log.warn(ChatColor.YELLOW + message);
    }

    override public void info(std::string message) {
        log.info(ChatColor.RESET + message);
    }

    override public void debug(std::string message) {
        log.debug(ChatColor.GRAY + "{}", message);
    }

    override public void debug(Object object) {
        log.debug("{}", object);
    }

    override public void debug(std::string message, Object... arguments) {

        log.debug(ChatColor.GRAY + std::string.format(message, arguments));
    }

    override public void setDebug(bool debug) {
        Configurator.setLevel(log.getName(), debug ? Level.DEBUG : Level.INFO);
    }

    override public bool isDebug() {
        return log.isDebugEnabled();
    }
}
