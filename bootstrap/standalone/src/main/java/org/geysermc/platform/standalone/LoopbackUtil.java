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
                    process = Runtime.getRuntime().exec(startScript);

                    geyserLogger.info(ChatColor.AQUA + LanguageUtils.getLocaleStringLog("geyser.bootstrap.loopback.added"));
                }
            } catch (Exception e) {
                e.printStackTrace();

                geyserLogger.error(LanguageUtils.getLocaleStringLog("geyser.bootstrap.loopback.failed"));
            }
        }
    }

}
