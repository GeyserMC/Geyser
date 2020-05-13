/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.platform.standalone;

import lombok.extern.log4j.Log4j2;

import net.minecrell.terminalconsole.SimpleTerminalConsole;

import org.apache.logging.log4j.core.config.Configurator;
import org.geysermc.common.ChatColor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;

@Log4j2
public class GeyserStandaloneLogger extends SimpleTerminalConsole implements org.geysermc.connector.GeyserLogger, CommandSender {

    private boolean colored = true;

    @Override
    protected boolean isRunning() {
        return !GeyserConnector.getInstance().isShuttingDown();
    }

    @Override
    protected void runCommand(String line) {
        GeyserConnector.getInstance().getCommandManager().runCommand(this, line);
    }

    @Override
    protected void shutdown() {
        GeyserConnector.getInstance().getBootstrap().onDisable();
    }

    @Override
    public void severe(String message) {
        log.fatal(printConsole(ChatColor.DARK_RED + message, colored));
    }

    @Override
    public void severe(String message, Throwable error) {
        log.fatal(printConsole(ChatColor.DARK_RED + message, colored), error);
    }

    @Override
    public void error(String message) {
        log.error(printConsole(ChatColor.RED + message, colored));
    }

    @Override
    public void error(String message, Throwable error) {
        log.error(printConsole(ChatColor.RED + message, colored), error);
    }

    @Override
    public void warning(String message) {
        log.warn(printConsole(ChatColor.YELLOW + message, colored));
    }

    @Override
    public void info(String message) {
        log.info(printConsole(ChatColor.WHITE + message, colored));
    }

    @Override
    public void debug(String message) {
        log.debug(printConsole(ChatColor.GRAY + message, colored));
    }

    public static String printConsole(String message, boolean colors) {
        return colors ? ChatColor.toANSI(message + ChatColor.RESET) : ChatColor.stripColors(message + ChatColor.RESET);
    }

    @Override
    public void setDebug(boolean debug) {
        Configurator.setLevel(log.getName(), debug ? org.apache.logging.log4j.Level.DEBUG : log.getLevel());
    }

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public void sendMessage(String message) {
        info(message);
    }

    @Override
    public boolean isConsole() {
        return true;
    }
}
