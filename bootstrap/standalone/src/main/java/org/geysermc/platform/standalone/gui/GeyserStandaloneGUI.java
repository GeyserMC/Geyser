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
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.platform.standalone.GeyserStandaloneLogger;
import org.geysermc.platform.standalone.command.GeyserCommandManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GeyserStandaloneGUI {

    private static final String[] playerTableHeadings = new String[] {"IP", "Username"};
    private static final List<Integer> ramValues = new ArrayList<>();

    private static final ColorPane consolePane = new ColorPane();
    private static final GraphPanel ramGraph = new GraphPanel();
    private static final JTable playerTable = new JTable(new String[][] { }, playerTableHeadings);

    private static final long  MEGABYTE = 1024L * 1024L;

    private JMenu commandsMenu;

    public GeyserStandaloneGUI() {
        // Create the frame and setup basic settings
        JFrame frame = new JFrame("Geyser Standalone");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(800, 400);
        frame.setMinimumSize(frame.getSize());

        // Remove Java UI look
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { }

        // Show a confirm dialog on close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we)
            {
                String buttons[] = {"Yes", "No"};
                int result = JOptionPane.showOptionDialog(frame, "Are you sure you want to exit?", frame.getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
                if (result == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        Container cp = frame.getContentPane();

        // Fetch and set the icon for the frame
        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("icon.png"));
        frame.setIconImage(icon.getImage());

        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(600);
        splitPane.addPropertyChangeListener("dividerLocation", e -> splitPaneLimit((JSplitPane)e.getSource()));
        splitPane.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                splitPaneLimit((JSplitPane)e.getSource());
            }
        });

        cp.add(splitPane, BorderLayout.CENTER);

        // Set the background and disable input for the text pane
        consolePane.setBackground(Color.BLACK);
        consolePane.setEditable(false);

        // Wrap the text pane in a scroll pane and add it to the form
        JScrollPane consoleScrollPane = new JScrollPane(consolePane);
        //cp.add(consoleScrollPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(consoleScrollPane);

        // Create a new menu bar for the top of the frame
        JMenuBar menuBar = new JMenuBar();

        // Create 'File'
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        // 'Open Geyser folder' button
        JMenuItem openButton = new JMenuItem("Open Geyser folder", KeyEvent.VK_O);
        openButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File("./"));
            } catch (IOException ioException) { }
        });
        fileMenu.add(openButton);

        fileMenu.addSeparator();

        // 'Exit' button
        JMenuItem exitButton = new JMenuItem("Exit", KeyEvent.VK_X);
        exitButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
        exitButton.addActionListener(e -> System.exit(0));
        fileMenu.add(exitButton);

        // Create 'Commands'
        commandsMenu = new JMenu("Commands");
        commandsMenu.setMnemonic(KeyEvent.VK_C);
        menuBar.add(commandsMenu);

        // Set the frames menu bar
        frame.setJMenuBar(menuBar);

        JPanel rightPane = new JPanel();
        rightPane.setLayout(new CardLayout(5, 5));
        //cp.add(rightPane, BorderLayout.EAST);
        splitPane.setRightComponent(rightPane);

        JPanel rightContentPane = new JPanel();
        rightContentPane.setLayout(new GridLayout(2, 1, 5, 5));
        rightPane.add(rightContentPane);

        // Set the ram graph to 0
        for (int i = 0; i < 10; i++) {
            ramValues.add(0);
        }
        ramGraph.setValues(ramValues);
        ramGraph.setXLabel("Loading...");
        rightContentPane.add(ramGraph);

        JScrollPane playerScrollPane = new JScrollPane(playerTable);
        rightContentPane.add(playerScrollPane);

        // This has to be done last
        frame.setVisible(true);
    }

    /**
     * Queue up an update to the text pane so we don't block the main thread
     *
     * @param text The text to append
     */
    private void updateTextPane(final String text) {
        SwingUtilities.invokeLater(() -> {
            consolePane.appendANSI(text);
            Document doc = consolePane.getDocument();
            consolePane.setCaretPosition(doc.getLength());
        });
    }

    /**
     * Redirect the default io streams to the text pane
     */
    public void redirectSystemStreams() {
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

        // Need to either get the log from jline3 or create a new ConsoleAppender
        // for log4j (2.4+) to fix this not getting the log outside of intellij
        // or set the property 'terminal.jline' to 'true'

        // None of this works :(

        //PropertiesUtil.getSystemProperties().put("terminal.jline", "true");

        /*
        LoggerContext cx = (LoggerContext) LogManager.getContext(true);
        Configuration conf = cx.getConfiguration();
        conf.getProperties().put("terminal.jline", "true");
        cx.setConfiguration(conf);
        */
    }

    /**
     * Add all the Geyser commands to the commands menu
     *
     * @param geyserStandaloneLogger The current logger
     * @param geyserCommandManager The commands manager
     */
    public void setupCommands(GeyserStandaloneLogger geyserStandaloneLogger, GeyserCommandManager geyserCommandManager) {
        commandsMenu.removeAll();

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

    public void startUpdateThread() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        Runnable periodicTask = () -> {
            if (GeyserConnector.getInstance() != null) {
                // Update player table
                String[][] playerNames = new String[GeyserConnector.getInstance().getPlayers().size()][2];
                int i = 0;
                for (Map.Entry<InetSocketAddress, GeyserSession> player : GeyserConnector.getInstance().getPlayers().entrySet()) {
                    playerNames[i][0] = player.getKey().getHostName();
                    playerNames[i][1] = player.getValue().getPlayerEntity().getUsername();

                    i++;
                }

                DefaultTableModel model = new DefaultTableModel(playerNames, playerTableHeadings);
                playerTable.setModel(model);
                model.fireTableDataChanged();
            }

            // Update ram graph
            final long freeMemory = Runtime.getRuntime().freeMemory();
            final long totalMemory = Runtime.getRuntime().totalMemory();
            final int freePercent = (int)(freeMemory * 100.0 / totalMemory + 0.5);
            ramValues.add(100 - freePercent);

            ramGraph.setXLabel("Usage: " + String.format("%,d", (totalMemory - freeMemory) / MEGABYTE) + "mb (" + freePercent + "% free)");

            // Trim the list
            int k = ramValues.size();
            if ( k > 10 )
                ramValues.subList(0, k - 10).clear();

            // Update the graph
            ramGraph.setValues(ramValues);
        };

        executor.scheduleAtFixedRate(periodicTask, 0, 1, TimeUnit.SECONDS);
    }

    private void splitPaneLimit(JSplitPane splitPane) {
        JRootPane frame = splitPane.getRootPane();
        int location = splitPane.getDividerLocation();
        if (location < frame.getWidth() - frame.getWidth() * 0.4f) {
            splitPane.setDividerLocation(Math.round(frame.getWidth() - frame.getWidth() * 0.4f));
        } else if (location > frame.getWidth() - 200) {
            splitPane.setDividerLocation(frame.getWidth() - 200);
        }
    }
}
