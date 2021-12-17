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

package org.geysermc.geyser;

import javax.swing.*;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Scanner;

public class GeyserMain {

    /**
     * Displays the run help message in the console and a message box if running with a gui
     */
    public void displayMessage() {
        String message = createMessage();

        if (System.console() == null && !isHeadless()) {
            JOptionPane.showMessageDialog(null, message, "GeyserMC Plugin: " + this.getPluginType(), JOptionPane.ERROR_MESSAGE);
        }

        printMessage(message);
    }

    /**
     * Load and format the run help text
     *
     * @return The formatted message
     */
    private String createMessage() {
        StringBuilder message = new StringBuilder();

        InputStream helpStream = GeyserMain.class.getClassLoader().getResourceAsStream("languages/run-help/" + Locale.getDefault().toString() + ".txt");

        if (helpStream == null) {
            helpStream = GeyserMain.class.getClassLoader().getResourceAsStream("languages/run-help/en_US.txt");
        }

        Scanner help = new Scanner(helpStream).useDelimiter("\\Z");
        String line = "";
        while (help.hasNext()) {
            line = help.next();

            line = line.replace("${plugin_type}", this.getPluginType());
            line = line.replace("${plugin_folder}", this.getPluginFolder());

            message.append(line).append("\n");
        }

        return message.toString();
    }

    /**
     * Check if we are in a headless environment
     *
     * @return Are we in a headless environment?
     */
    private boolean isHeadless() {
        try {
            Class<?> graphicsEnvironment = Class.forName("java.awt.GraphicsEnvironment");
            Method isHeadless = graphicsEnvironment.getDeclaredMethod("isHeadless");
            return (Boolean)isHeadless.invoke(null);
        } catch (Exception ignored) {
        }

        return true;
    }

    /**
     * Simply print a message to console
     *
     * @param message The message to print
     */
    private void printMessage(String message) {
        System.out.print(message);
    }

    /**
     * Get the platform the plugin is for
     *
     * @return The string representation of the plugin platforms name
     */
    public String getPluginType() {
        return "unknown";
    }

    /**
     * Get the folder name the plugin should go into
     *
     * @return The string representation of the folder
     */
    public String getPluginFolder() {
        return "unknown";
    }
}
