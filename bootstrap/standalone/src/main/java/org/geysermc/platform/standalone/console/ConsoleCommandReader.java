/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.standalone.console;

import net.minecrell.terminalconsole.TerminalConsoleAppender;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.GeyserConsoleCommandSender;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsoleCommandReader {

    private GeyserConnector connector;
    private Terminal terminal;

    public ConsoleCommandReader(GeyserConnector connector) {
        this.connector = connector;
        this.terminal = TerminalConsoleAppender.getTerminal();
    }

    public void startConsole() {
        Thread thread = new Thread(() -> {
            if (terminal != null) {
                LineReader lineReader = LineReaderBuilder.builder()
                        .appName("Geyser")
                        .terminal(terminal)
                        .build();
                TerminalConsoleAppender.setReader(lineReader);

                try {
                    String line;

                    while (true) {
                        try {
                            line = lineReader.readLine("> ");
                        } catch (EndOfFileException ignored) {
                            continue;
                        }

                        if (line == null)
                            break;
                    }
                } catch (UserInterruptException ignore) {
                    /* do nothing */
                } finally {
                    TerminalConsoleAppender.setReader(null);
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        GeyserConsoleCommandSender sender = new GeyserConsoleCommandSender();
                        connector.getCommandMap().runCommand(sender, line);
                    }
                } catch (IOException ex) {
                    Logger.getLogger("Geyser").log(Level.SEVERE, null, ex);
                }
            }
        });

        thread.setName("ConsoleCommandThread");
    }
}
