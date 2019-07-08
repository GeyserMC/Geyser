/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view the LICENCE file for details.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.console;

import org.geysermc.api.command.ConsoleCommandSender;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.GeyserConsoleCommandSender;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
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
    private Thread thread;

    public ConsoleCommandReader(GeyserConnector connector) {
        this.connector = connector;
        this.terminal = TerminalConsoleAppender.getTerminal();
    }

    public void startConsole() {
        thread = new Thread() {
            @Override
            public void run() {
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
                    } catch (UserInterruptException e /* do nothing */) {

                    } finally {
                        TerminalConsoleAppender.setReader(null);
                    }
                } else {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            ConsoleCommandSender sender = new GeyserConsoleCommandSender();
                            connector.getCommandMap().runCommand(sender, line);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger("Geyser").log(Level.SEVERE, null, ex);
                    }
                }
            }
        };

        thread.setName("ConsoleCommandThread");
        connector.getGeneralThreadPool().execute(thread);
    }
}
