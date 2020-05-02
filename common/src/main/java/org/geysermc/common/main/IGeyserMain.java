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

package org.geysermc.common.main;

import javax.swing.*;
import java.io.InputStream;
import java.util.Scanner;

public class IGeyserMain {

    public void displayMessage() {
        String message = createMessage();

        if (System.console() == null) {
            JOptionPane.showMessageDialog(null, message, "GeyserMC Plugin: " + this.getPluginType(), JOptionPane.ERROR_MESSAGE);
        }

        printMessage(message);
    }

    private String createMessage() {
        String message = "";

        InputStream helpStream = IGeyserMain.class.getClassLoader().getResourceAsStream("help.txt");
        Scanner help = new Scanner(helpStream).useDelimiter("\\Z");
        String line = "";
        while (help.hasNext()) {
            line = help.next();

            line = line.replace("${plugin_type}", this.getPluginType());
            line = line.replace("${plugin_folder}", this.getPluginFolder());

            message += line + "\n";
        }

        return message;
    }

    private void printMessage(String message) {
        System.out.print(message);
    }

    public String getPluginType() {
        return "unknown";
    }

    public String getPluginFolder() {
        return "unknown";
    }
}
