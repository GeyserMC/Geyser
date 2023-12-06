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

package org.geysermc.geyser.util;

import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class LoopbackUtil {
    private static final String checkExemption = "CheckNetIsolation LoopbackExempt -s";
    private static final String loopbackCommand = "CheckNetIsolation LoopbackExempt -a -n=Microsoft.MinecraftUWP_8wekyb3d8bbwe";
    /**
     * This string needs to be checked in the event Minecraft is not installed - no Minecraft string will be present in the checkExemption command.
     */
    private static final String minecraftApplication = "S-1-15-2-1958404141-86561845-1752920682-3514627264-368642714-62675701-733520436";
    private static final String startScript = "powershell -Command \"Start-Process 'cmd' -ArgumentList /c,%temp%/loopback_minecraft.bat -Verb runAs\"";

    /**
     * @return true if loopback is not addressed properly.
     */
    public static boolean needsLoopback(GeyserLogger logger) {
        String os = System.getProperty("os.name");
        if (os.equalsIgnoreCase("Windows 10") || os.equalsIgnoreCase("Windows 11")) {
            try {
                Process process = Runtime.getRuntime().exec(checkExemption);
                InputStream is = process.getInputStream();

                int data;
                StringBuilder sb = new StringBuilder();
                while ((data = is.read()) != -1) {
                    sb.append((char) data);
                }

                return !sb.toString().contains(minecraftApplication);
            } catch (Exception e) {
                logger.error("Couldn't detect if loopback has been added on Windows!", e);
                return true;
            }
        }
        return false;
    }

    public static void checkAndApplyLoopback(GeyserLogger geyserLogger) {
        if (needsLoopback(geyserLogger)) {
            try {
                Files.write(Paths.get(System.getenv("temp") + "/loopback_minecraft.bat"), loopbackCommand.getBytes());
                Runtime.getRuntime().exec(startScript);

                geyserLogger.info(ChatColor.AQUA + GeyserLocale.getLocaleStringLog("geyser.bootstrap.loopback.added"));
            } catch (Exception e) {
                e.printStackTrace();

                geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.loopback.failed"));
            }
        }
    }

    private LoopbackUtil() {
    }
}
