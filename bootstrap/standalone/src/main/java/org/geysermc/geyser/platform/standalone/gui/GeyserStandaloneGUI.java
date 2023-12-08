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

package org.geysermc.geyser.platform.standalone.gui;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GeyserStandaloneGUI {

    private static final long MEGABYTE = 1024L * 1024L;

    private final GeyserLogger logger;

    private final ColorPane consolePane = new ColorPane();
    private final int originalFontSize = consolePane.getFont().getSize();
    private final JTextField commandInput = new JTextField();
    private final CommandListener commandListener = new CommandListener();

    private final GraphPanel ramGraph = new GraphPanel();
    private final List<Integer> ramValues = new ArrayList<>();

    private final DefaultTableModel playerTableModel = new DefaultTableModel();

    /**
     * Create and show the Geyser-Standalone GUI
     *
     * @param logger the logger for determining debug mode, and executing commands from the console
     */
    public GeyserStandaloneGUI(GeyserLogger logger) {
        this.logger = logger;

        // Create the frame and setup basic settings
        JFrame frame = new JFrame(GeyserLocale.getLocaleStringLog("geyser.gui.title"));
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(800, 400);
        frame.setMinimumSize(frame.getSize());

        // Remove Java UI look
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        // Show a confirm dialog on close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                String[] buttons = {GeyserLocale.getLocaleStringLog("geyser.gui.exit.confirm"), GeyserLocale.getLocaleStringLog("geyser.gui.exit.deny")};
                int result = JOptionPane.showOptionDialog(frame, GeyserLocale.getLocaleStringLog("geyser.gui.exit.message"), frame.getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
                if (result == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        Container cp = frame.getContentPane();

        // Fetch and set the icon for the frame
        URL image = getClass().getClassLoader().getResource("icon.png");
        if (image != null) {
            ImageIcon icon = new ImageIcon(image);
            frame.setIconImage(icon.getImage());
        }

        // File, View, Options, etc
        setupMenuBar(frame);

        // Setup the split pane and event listeners
        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(600);
        splitPane.addPropertyChangeListener("dividerLocation", e -> splitPaneLimit((JSplitPane) e.getSource()));
        splitPane.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                splitPaneLimit((JSplitPane) e.getSource());
            }
        });

        cp.add(splitPane, BorderLayout.CENTER);

        // Holds console and command input components
        JPanel leftPane = new JPanel(new BorderLayout());
        splitPane.setLeftComponent(leftPane);

        // Set the background and disable editing of the console
        consolePane.setBackground(Color.BLACK);
        consolePane.setEditable(false);

        // Wrap the text pane in a scroll pane and add it to the form
        JScrollPane consoleScrollPane = new JScrollPane(consolePane);
        leftPane.add(consoleScrollPane, BorderLayout.CENTER);

        // a bit taller than the default layout - width is ignored fortunately
        commandInput.setPreferredSize(new Dimension(100, 25));
        commandInput.setEnabled(false); // disabled until command handler is initialized
        commandInput.addActionListener(commandListener);
        leftPane.add(commandInput, BorderLayout.SOUTH);

        JPanel rightPane = new JPanel();
        rightPane.setLayout(new CardLayout(5, 5));
        splitPane.setRightComponent(rightPane);

        JPanel rightContentPane = new JPanel();
        rightContentPane.setLayout(new GridLayout(2, 1, 5, 5));
        rightPane.add(rightContentPane);

        // Set the ram graph to 0
        for (int i = 0; i < 10; i++) {
            ramValues.add(0);
        }
        ramGraph.setValues(ramValues);
        ramGraph.setXLabel(GeyserLocale.getLocaleStringLog("geyser.gui.graph.loading"));
        rightContentPane.add(ramGraph);

        playerTableModel.addColumn(GeyserLocale.getLocaleStringLog("geyser.gui.table.ip"));
        playerTableModel.addColumn(GeyserLocale.getLocaleStringLog("geyser.gui.table.username"));

        JTable playerTable = new JTable(playerTableModel);
        JScrollPane playerScrollPane = new JScrollPane(playerTable);
        rightContentPane.add(playerScrollPane);

        // This has to be done last
        frame.setVisible(true);
    }

    private void setupMenuBar(JFrame frame) {
        // Create a new menu bar for the top of the frame
        JMenuBar menuBar = new JMenuBar();

        // Create 'File'
        JMenu fileMenu = new JMenu(GeyserLocale.getLocaleStringLog("geyser.gui.menu.file"));
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        // 'Open Geyser folder' button
        JMenuItem openButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.file.open_folder"), KeyEvent.VK_O);
        openButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File("./"));
            } catch (IOException ignored) { }
        });
        fileMenu.add(openButton);

        fileMenu.addSeparator();

        // 'Exit' button
        JMenuItem exitButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.file.exit"), KeyEvent.VK_X);
        exitButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
        exitButton.addActionListener(e -> System.exit(0));
        fileMenu.add(exitButton);

        // Create 'View'
        JMenu viewMenu = new JMenu(GeyserLocale.getLocaleStringLog("geyser.gui.menu.view"));
        viewMenu.setMnemonic(KeyEvent.VK_V);
        menuBar.add(viewMenu);

        // 'Zoom in' button
        JMenuItem zoomInButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.view.zoom_in"));
        zoomInButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));
        zoomInButton.addActionListener(e -> consolePane.setFont(new Font(consolePane.getFont().getName(), consolePane.getFont().getStyle(), consolePane.getFont().getSize() + 1)));
        viewMenu.add(zoomInButton);

        // 'Zoom in' button
        JMenuItem zoomOutButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.view.zoom_out"));
        zoomOutButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        zoomOutButton.addActionListener(e -> consolePane.setFont(new Font(consolePane.getFont().getName(), consolePane.getFont().getStyle(), consolePane.getFont().getSize() - 1)));
        viewMenu.add(zoomOutButton);

        // 'Reset Zoom' button
        JMenuItem resetZoomButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.view.reset_zoom"));
        resetZoomButton.addActionListener(e -> consolePane.setFont(new Font(consolePane.getFont().getName(), consolePane.getFont().getStyle(), originalFontSize)));
        viewMenu.add(resetZoomButton);

        // create 'Options'
        JMenu optionsMenu = new JMenu(GeyserLocale.getLocaleStringLog("geyser.gui.menu.options"));
        viewMenu.setMnemonic(KeyEvent.VK_O);
        menuBar.add(optionsMenu);

        JCheckBoxMenuItem debugMode = new JCheckBoxMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.options.toggle_debug_mode"));
        debugMode.setSelected(logger.isDebug());
        debugMode.addActionListener(e -> logger.setDebug(debugMode.getState()));
        optionsMenu.add(debugMode);

        // Set the frames menu bar
        frame.setJMenuBar(menuBar);
    }

    /**
     * Queue up an update to the text pane so we don't block the main thread
     *
     * @param text The text to append
     */
    private void appendConsole(final String text) {
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
                appendConsole(String.valueOf((char) b));
            }

            @Override
            public void write(byte @NonNull [] b, int off, int len) {
                appendConsole(new String(b, off, len));
            }

            @Override
            public void write(byte @NonNull[] b) {
                write(b, 0, b.length);
            }
        };

        // Override the system output streams
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));

    }

    /**
     * Enable the command input box.
     *
     * @param executor the executor for running commands off the GUI thread
     * @param commandManager the command manager to delegate commands to
     */
    public void enableCommands(ScheduledExecutorService executor, GeyserCommandManager commandManager) {
        // we don't want to block the GUI thread with the command execution
        // todo: once cloud is used, an AsynchronousCommandExecutionCoordinator can be used to avoid this scheduler
        commandListener.handler = cmd -> executor.schedule(() -> commandManager.runCommand(logger, cmd), 0, TimeUnit.SECONDS);
        commandInput.setEnabled(true);
        commandInput.requestFocusInWindow();
    }

    /**
     * Start the thread to update the form information every 1s
     */
    public void startUpdateThread() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        Runnable periodicTask = () -> {
            if (GeyserImpl.getInstance() != null) {
                // Update player table
                playerTableModel.getDataVector().removeAllElements();

                for (GeyserSession player : GeyserImpl.getInstance().getSessionManager().getSessions().values()) {
                    Vector<String> row = new Vector<>();
                    row.add(player.getSocketAddress().getHostName());
                    row.add(player.getPlayerEntity().getUsername());

                    playerTableModel.addRow(row);
                }

                playerTableModel.fireTableDataChanged();
            }

            // Update ram graph
            final long freeMemory = Runtime.getRuntime().freeMemory();
            final long totalMemory = Runtime.getRuntime().totalMemory();
            final int freePercent = (int) (freeMemory * 100.0 / totalMemory + 0.5);
            ramValues.add(100 - freePercent);

            ramGraph.setXLabel(GeyserLocale.getLocaleStringLog("geyser.gui.graph.usage", String.format("%,d", (totalMemory - freeMemory) / MEGABYTE), freePercent));

            // Trim the list
            int k = ramValues.size();
            if (k > 10)
                ramValues.subList(0, k - 10).clear();

            // Update the graph
            ramGraph.setValues(ramValues);
        };

        // SwingUtilities.invokeLater is called so we don't run into threading issues with the GUI
        executor.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(periodicTask), 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Make sure the JSplitPane divider is within a set of bounds
     *
     * @param splitPane The JSplitPane to check
     */
    private void splitPaneLimit(JSplitPane splitPane) {
        JRootPane frame = splitPane.getRootPane();
        int location = splitPane.getDividerLocation();
        if (location < frame.getWidth() - frame.getWidth() * 0.4f) {
            splitPane.setDividerLocation(Math.round(frame.getWidth() - frame.getWidth() * 0.4f));
        } else if (location > frame.getWidth() - 200) {
            splitPane.setDividerLocation(frame.getWidth() - 200);
        }
    }

    private class CommandListener implements ActionListener {

        private Consumer<String> handler;

        @Override
        public void actionPerformed(ActionEvent e) {
            String command = commandInput.getText();
            appendConsole(command + "\n"); // show what was run in the console
            handler.accept(command); // run the command
            commandInput.setText(""); // clear the input
        }
    }
}
