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

package org.geysermc.platform.standalone.gui;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.platform.standalone.GeyserStandaloneLogger;
import org.geysermc.platform.standalone.command.GeyserCommandManager;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

public class GeyserStandaloneGUI {

    private static final ColorPane textPane = new ColorPane();

    private JMenu commandsMenu;

    /**
     * Queue up an update to the text pane so we don't block the main thread
     *
     * @param text The text to append
     */
    private void updateTextPane(final String text) {
        SwingUtilities.invokeLater(() -> {
            textPane.appendANSI(text);
            Document doc = textPane.getDocument();
            textPane.setCaretPosition(doc.getLength());
        });
    }

    public void redirectSystemStreams() {
        // Create the frame and setup basic settings
        JFrame frame = new JFrame("Geyser Standalone");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        Container cp = frame.getContentPane();

        // Fetch and set the icon for the frame
        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("icon.png"));
        frame.setIconImage(icon.getImage());

        // Set the background and disable input for the text pane
        textPane.setBackground(Color.BLACK);
        textPane.setEditable(false);

        // Wrap the text pane in a scroll pane and add it to the form
        JScrollPane scrollPane = new JScrollPane(textPane);
        cp.add(scrollPane, BorderLayout.CENTER);

        // Create a new menu bar for the top of the frame
        JMenuBar menuBar = new JMenuBar();

        // Create 'File'
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        // 'Exit' button
        JMenuItem exitButton = new JMenuItem("Exit", KeyEvent.VK_X);
        exitButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
        exitButton.addActionListener(e -> GeyserConnector.getInstance().getBootstrap().onDisable());
        fileMenu.add(exitButton);

        // Create 'Commands'
        commandsMenu = new JMenu("Commands");
        commandsMenu.setMnemonic(KeyEvent.VK_C);
        menuBar.add(commandsMenu);

        // Set the frames menu bar
        frame.setJMenuBar(menuBar);

        // This has to be done last
        frame.setVisible(true);

        // Setup a new output stream to forward it to the text pane
        OutputStream out = new OutputStream() {
            @Override
            public void write(final int b) {
                updateTextPane(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                updateTextPane(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) {
                write(b, 0, b.length);
            }
        };

        // Override the system output streams
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    /**
     * Add all the Geyser commands to the commands menu
     *
     * @param geyserStandaloneLogger The current logger
     * @param geyserCommandManager The commands manager
     */
    public void setupCommands(GeyserStandaloneLogger geyserStandaloneLogger, GeyserCommandManager geyserCommandManager) {
        for (Map.Entry<String, GeyserCommand> command : geyserCommandManager.getCommands().entrySet()) {
            // Remove the offhand command and any alias commands to prevent duplicates in the list
            if ("offhand".equals(command.getValue().getName()) || command.getValue().getAliases().contains(command.getKey())) {
                continue;
            }

            // Create the button that runs the command
            JMenuItem commandButton = new JMenuItem(command.getValue().getName());
            commandButton.getAccessibleContext().setAccessibleDescription(command.getValue().getDescription());
            commandButton.addActionListener(e -> command.getValue().execute(geyserStandaloneLogger, new String[]{ }));
            commandsMenu.add(commandButton);
        }
    }
}
