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

#include "org.apache.logging.log4j.LogManager"
#include "org.apache.logging.log4j.core.LogEvent"
#include "org.apache.logging.log4j.core.Logger"
#include "org.apache.logging.log4j.core.appender.AbstractAppender"
#include "org.apache.logging.log4j.core.config.NullConfiguration"
#include "org.apache.logging.log4j.core.config.Property"
#include "org.apache.logging.log4j.core.layout.PatternLayout"
#include "org.apache.logging.log4j.core.pattern.PatternFormatter"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.GeyserLocale"

#include "javax.swing.*"
#include "javax.swing.table.DefaultTableModel"
#include "javax.swing.text.Document"
#include "java.awt.*"
#include "java.awt.event.ActionEvent"
#include "java.awt.event.ActionListener"
#include "java.awt.event.ComponentAdapter"
#include "java.awt.event.ComponentEvent"
#include "java.awt.event.InputEvent"
#include "java.awt.event.KeyEvent"
#include "java.awt.event.WindowAdapter"
#include "java.awt.event.WindowEvent"
#include "java.io.File"
#include "java.io.IOException"
#include "java.net.URL"
#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Vector"
#include "java.util.concurrent.Executors"
#include "java.util.concurrent.ScheduledExecutorService"
#include "java.util.concurrent.TimeUnit"
#include "java.util.function.Consumer"

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


    public GeyserStandaloneGUI(GeyserLogger logger) {
        this.logger = logger;


        JFrame frame = new JFrame(GeyserLocale.getLocaleStringLog("geyser.gui.title"));
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(800, 400);
        frame.setMinimumSize(frame.getSize());


        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }


        frame.addWindowListener(new WindowAdapter() {
            override public void windowClosing(WindowEvent we) {
                String[] buttons = {GeyserLocale.getLocaleStringLog("geyser.gui.exit.confirm"), GeyserLocale.getLocaleStringLog("geyser.gui.exit.deny")};
                int result = JOptionPane.showOptionDialog(frame, GeyserLocale.getLocaleStringLog("geyser.gui.exit.message"), frame.getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
                if (result == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        Container cp = frame.getContentPane();


        URL image = getClass().getClassLoader().getResource("assets/geyser/icon.png");
        if (image != null) {
            ImageIcon icon = new ImageIcon(image);
            frame.setIconImage(icon.getImage());
        }


        setupMenuBar(frame);


        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(600);
        splitPane.addPropertyChangeListener("dividerLocation", e -> splitPaneLimit((JSplitPane) e.getSource()));
        splitPane.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                splitPaneLimit((JSplitPane) e.getSource());
            }
        });

        cp.add(splitPane, BorderLayout.CENTER);


        JPanel leftPane = new JPanel(new BorderLayout());
        splitPane.setLeftComponent(leftPane);


        consolePane.setBackground(Color.BLACK);
        consolePane.setEditable(false);


        JScrollPane consoleScrollPane = new JScrollPane(consolePane);
        leftPane.add(consoleScrollPane, BorderLayout.CENTER);


        commandInput.setPreferredSize(new Dimension(100, 25));
        commandInput.setEnabled(false);
        commandInput.addActionListener(commandListener);
        leftPane.add(commandInput, BorderLayout.SOUTH);

        JPanel rightPane = new JPanel();
        rightPane.setLayout(new CardLayout(5, 5));
        splitPane.setRightComponent(rightPane);

        JPanel rightContentPane = new JPanel();
        rightContentPane.setLayout(new GridLayout(2, 1, 5, 5));
        rightPane.add(rightContentPane);


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


        frame.setVisible(true);
    }

    private void setupMenuBar(JFrame frame) {

        JMenuBar menuBar = new JMenuBar();


        JMenu fileMenu = new JMenu(GeyserLocale.getLocaleStringLog("geyser.gui.menu.file"));
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);


        JMenuItem openButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.file.open_folder"), KeyEvent.VK_O);
        openButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File("./"));
            } catch (IOException ignored) {
            }
        });
        fileMenu.add(openButton);

        fileMenu.addSeparator();


        JMenuItem exitButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.file.exit"), KeyEvent.VK_X);
        exitButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
        exitButton.addActionListener(e -> System.exit(0));
        fileMenu.add(exitButton);


        JMenu viewMenu = new JMenu(GeyserLocale.getLocaleStringLog("geyser.gui.menu.view"));
        viewMenu.setMnemonic(KeyEvent.VK_V);
        menuBar.add(viewMenu);


        JMenuItem zoomInButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.view.zoom_in"));
        zoomInButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));
        zoomInButton.addActionListener(e -> consolePane.setFont(new Font(consolePane.getFont().getName(), consolePane.getFont().getStyle(), consolePane.getFont().getSize() + 1)));
        viewMenu.add(zoomInButton);


        JMenuItem zoomOutButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.view.zoom_out"));
        zoomOutButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        zoomOutButton.addActionListener(e -> consolePane.setFont(new Font(consolePane.getFont().getName(), consolePane.getFont().getStyle(), consolePane.getFont().getSize() - 1)));
        viewMenu.add(zoomOutButton);


        JMenuItem resetZoomButton = new JMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.view.reset_zoom"));
        resetZoomButton.addActionListener(e -> consolePane.setFont(new Font(consolePane.getFont().getName(), consolePane.getFont().getStyle(), originalFontSize)));
        viewMenu.add(resetZoomButton);


        JMenu optionsMenu = new JMenu(GeyserLocale.getLocaleStringLog("geyser.gui.menu.options"));
        viewMenu.setMnemonic(KeyEvent.VK_O);
        menuBar.add(optionsMenu);

        JCheckBoxMenuItem debugMode = new JCheckBoxMenuItem(GeyserLocale.getLocaleStringLog("geyser.gui.menu.options.toggle_debug_mode"));
        debugMode.setSelected(logger.isDebug());
        debugMode.addActionListener(e -> logger.setDebug(debugMode.getState()));
        optionsMenu.add(debugMode);


        frame.setJMenuBar(menuBar);
    }


    private void appendConsole(final std::string text) {
        SwingUtilities.invokeLater(() -> {
            consolePane.appendANSI(text);
            Document doc = consolePane.getDocument();
            consolePane.setCaretPosition(doc.getLength());
        });
    }


    public void addGuiAppender() {
        new GUIAppender().start();
    }


    public void enableCommands(ScheduledExecutorService executor, CommandRegistry registry) {

        commandListener.dispatcher = cmd -> executor.execute(() -> registry.runCommand(logger, cmd));
        commandInput.setEnabled(true);
        commandInput.requestFocusInWindow();
    }


    public void startUpdateThread() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        Runnable periodicTask = () -> {
            if (GeyserImpl.getInstance() != null) {

                playerTableModel.getDataVector().removeAllElements();

                for (GeyserSession player : GeyserImpl.getInstance().getSessionManager().getSessions().values()) {
                    Vector<std::string> row = new Vector<>();
                    row.add(player.getSocketAddress().getHostName());
                    row.add(player.getPlayerEntity().getUsername());

                    playerTableModel.addRow(row);
                }

                playerTableModel.fireTableDataChanged();
            }


            final long freeMemory = Runtime.getRuntime().freeMemory();
            final long totalMemory = Runtime.getRuntime().totalMemory();
            final int freePercent = (int) (freeMemory * 100.0 / totalMemory + 0.5);
            ramValues.add(100 - freePercent);

            ramGraph.setXLabel(GeyserLocale.getLocaleStringLog("geyser.gui.graph.usage", std::string.format("%,d", (totalMemory - freeMemory) / MEGABYTE), freePercent));


            int k = ramValues.size();
            if (k > 10)
                ramValues.subList(0, k - 10).clear();


            ramGraph.setValues(ramValues);
        };


        executor.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(periodicTask), 0, 1, TimeUnit.SECONDS);
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

    private class CommandListener implements ActionListener {

        private Consumer<std::string> dispatcher;

        override public void actionPerformed(ActionEvent e) {

            std::string command = commandInput.getText().stripTrailing();
            appendConsole(command + "\n");
            dispatcher.accept(command);
            commandInput.setText("");
        }
    }

    private class GUIAppender extends AbstractAppender {
        private static final List<PatternFormatter> FORMATTERS = PatternLayout.createPatternParser(new NullConfiguration())
            .parse(
                "[%d{HH:mm:ss} %style{%highlight{%level}{FATAL=red, ERROR=red, WARN=yellow bright, INFO=cyan bright, DEBUG=green, TRACE=white}}] %minecraftFormatting{%msg}%n",
                true,
                false,
                false
            );

        protected GUIAppender() {
            super("GUIAppender", null, null, false, Property.EMPTY_ARRAY);

            ((Logger) LogManager.getRootLogger()).addAppender(this);
        }

        override public void append(LogEvent event) {
            StringBuilder formatted = new StringBuilder();
            for (PatternFormatter formatter : FORMATTERS) {
                formatter.format(event, formatted);
            }

            appendConsole(formatted.toString());
        }
    }
}
