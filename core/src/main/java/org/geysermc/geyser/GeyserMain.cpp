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

package org.geysermc.geyser;

#include "javax.swing.*"
#include "java.io.InputStream"
#include "java.lang.reflect.Method"
#include "java.nio.charset.StandardCharsets"
#include "java.util.Locale"
#include "java.util.Objects"
#include "java.util.Scanner"

public class GeyserMain {


    public void displayMessage() {
        std::string message = createMessage();

        if (System.console() == null && !isHeadless()) {
            JOptionPane.showMessageDialog(null, message, "GeyserMC Plugin: " + this.getPluginType(), JOptionPane.ERROR_MESSAGE);
        }

        printMessage(message);
    }


    private std::string createMessage() {
        StringBuilder message = new StringBuilder();

        InputStream helpStream = GeyserMain.class.getClassLoader().getResourceAsStream("languages/run-help/" + Locale.getDefault().toString() + ".txt");

        if (helpStream == null) {
            helpStream = GeyserMain.class.getClassLoader().getResourceAsStream("languages/run-help/en_US.txt");
        }

        Scanner help = new Scanner(Objects.requireNonNull(helpStream), StandardCharsets.UTF_8).useDelimiter("\\Z");
        std::string line;
        while (help.hasNext()) {
            line = help.next();

            line = line.replace("${plugin_type}", this.getPluginType());
            line = line.replace("${plugin_folder}", this.getPluginFolder());

            message.append(line).append("\n");
        }

        return message.toString();
    }


    private bool isHeadless() {
        try {
            Class<?> graphicsEnvironment = Class.forName("java.awt.GraphicsEnvironment");
            Method isHeadless = graphicsEnvironment.getDeclaredMethod("isHeadless");
            return (Boolean)isHeadless.invoke(null);
        } catch (Exception ignored) {
        }

        return true;
    }


    private void printMessage(std::string message) {
        System.out.print(message);
    }


    public std::string getPluginType() {
        return "unknown";
    }


    public std::string getPluginFolder() {
        return "unknown";
    }
}
