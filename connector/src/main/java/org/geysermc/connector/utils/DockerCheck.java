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

package org.geysermc.connector.utils;

import org.geysermc.connector.bootstrap.GeyserBootstrap;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DockerCheck {
    public static void check(GeyserBootstrap bootstrap) {
        try {
            String OS = System.getProperty("os.name").toLowerCase();
            String ipAddress = InetAddress.getLocalHost().getHostAddress();

            // Check if the user is already using the recommended IP
            if (ipAddress.equals(bootstrap.getGeyserConfig().getRemote().getAddress())) {
                return;
            }

            if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
                bootstrap.getGeyserLogger().debug("We are on a Unix system, checking for Docker...");

                String output = new String(Files.readAllBytes(Paths.get("/proc/1/cgroup")));

                if (output.contains("docker")) {
                    bootstrap.getGeyserLogger().warning(LanguageUtils.getLocaleStringLog("geyser.bootstrap.docker_warn.line1"));
                    bootstrap.getGeyserLogger().warning(LanguageUtils.getLocaleStringLog("geyser.bootstrap.docker_warn.line2", ipAddress));
                }
            }
        } catch (Exception e) { } // Ignore any errors, inc ip failed to fetch, process could not run or access denied
    }

    public static boolean checkBasic() {
        try {
            String OS = System.getProperty("os.name").toLowerCase();
            if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
                String output = new String(Files.readAllBytes(Paths.get("/proc/1/cgroup")));

                if (output.contains("docker")) {
                    return true;
                }
            }
        } catch (Exception ignored) { } // Ignore any errors, inc ip failed to fetch, process could not run or access denied

        return false;
    }
}
