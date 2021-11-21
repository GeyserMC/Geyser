/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import lombok.extern.log4j.Log4j2;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.command.CommandSender;
import org.geysermc.geyser.text.ChatColor;

@Log4j2
public class GeyserStandaloneLogger extends SimpleTerminalConsole implements GeyserLogger, CommandSender {

    @Override
    protected boolean isRunning() {
        return !GeyserImpl.getInstance().isShuttingDown();
    }

    @Override
    protected void runCommand(String line) {
        GeyserImpl.getInstance().getCommandManager().runCommand(this, line);
    }

    @Override
    protected void shutdown() {
        GeyserImpl.getInstance().getBootstrap().onDisable();
    }

    @Override
    public void severe(String message) {
        log.fatal(ChatColor.DARK_RED + message);
    }

    @Override
    public void severe(String message, Throwable error) {
        log.fatal(ChatColor.DARK_RED + message, error);
    }

    @Override
    public void error(String message) {
        log.error(ChatColor.RED + message);
    }

    @Override
    public void error(String message, Throwable error) {
        log.error(ChatColor.RED + message, error);
    }

    @Override
    public void warning(String message) {
        log.warn(ChatColor.YELLOW + message);
    }

    @Override
    public void info(String message) {
        log.info(ChatColor.RESET + message);
    }

    @Override
    public void debug(String message) {
        log.debug(ChatColor.GRAY + message);
    }

    @Override
    public void setDebug(boolean debug) {
        Configurator.setLevel(log.getName(), debug ? Level.DEBUG : Level.INFO);
    }

    public boolean isDebug() {
        return log.isDebugEnabled();
    }

    @Override
    public String name() {
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

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }
}
