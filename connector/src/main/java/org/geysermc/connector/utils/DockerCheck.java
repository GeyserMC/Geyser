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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

public class DockerCheck {
    public static void Check(GeyserBootstrap bootstrap) {
        try {
            String OS = System.getProperty("os.name").toLowerCase();
            String ipAddress = InetAddress.getLocalHost().getHostAddress();

            // Check if the user is already using the recommended IP
            if (ipAddress.equals(bootstrap.getGeyserConfig().getRemote().getAddress())) {
                return;
            }

            if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
                bootstrap.getGeyserLogger().debug("We are on a unix system, checking for docker...");

                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("bash", "-c", "cat /proc/1/cgroup");

                Process process = processBuilder.start();

                StringBuilder output = new StringBuilder();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line + "\n");
                }

                int exitVal = process.waitFor();
                if (exitVal == 0) {
                    if (output.toString().contains("docker")) {
                        bootstrap.getGeyserLogger().warning("You are most likely in a docker container, this may cause connection issues from geyser to java");
                        bootstrap.getGeyserLogger().warning("We recommended using the following IP as the remote address: " + ipAddress);
                    }
                }
            }
        } catch (Exception e) { } // Ignore any errors, inc ip failed to fetch, process could not run or access denied
    }
}
