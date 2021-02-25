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

package org.geysermc.platform.standalone;

import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.utils.LanguageUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;

public class LoopbackUtil {
    private static final String checkExemption = "powershell -Command \"CheckNetIsolation LoopbackExempt -s\""; // Java's Exec feature runs as CMD, NetIsolation is only accessible from PowerShell.
    private static final String loopbackCommand = "powershell -Command \"CheckNetIsolation LoopbackExempt -a -n='Microsoft.MinecraftUWP_8wekyb3d8bbwe'\"";
    private static final String startScript = "powershell -Command \"Start-Process 'cmd' -ArgumentList /c,%temp%/loopback_minecraft.bat -Verb runAs\"";

    public static void checkLoopback(GeyserStandaloneLogger geyserLogger) {
        if (System.getProperty("os.name").equalsIgnoreCase("Windows 10")) {
            try {
                Process process = Runtime.getRuntime().exec(checkExemption);
                InputStream is = process.getInputStream();
                StringBuilder sb = new StringBuilder();

                while (process.isAlive()) {
                    if (is.available() != 0) {
                        sb.append((char) is.read());
                    }
                }

                String result = sb.toString();

                if (!result.contains("minecraftuwp")) {
                    Files.write(Paths.get(System.getenv("temp") + "/loopback_minecraft.bat"), loopbackCommand.getBytes(), new OpenOption[0]);
                    Runtime.getRuntime().exec(startScript);

                    geyserLogger.info(ChatColor.AQUA + LanguageUtils.getLocaleStringLog("geyser.bootstrap.loopback.added"));
                }
            } catch (Exception e) {
                e.printStackTrace();

                geyserLogger.error(LanguageUtils.getLocaleStringLog("geyser.bootstrap.loopback.failed"));
            }
        }
    }

}
