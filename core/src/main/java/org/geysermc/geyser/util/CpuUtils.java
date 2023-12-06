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

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

public final class CpuUtils {

    public static String tryGetProcessorName() {
        try {
            if (new File("/proc/cpuinfo").canRead()) {
                return getLinuxProcessorName();
            } else {
                return getWindowsProcessorName();
            }
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * Much of the code here was copied from the OSHI project. This is simply stripped down to only get the CPU model.
     * <a href="https://github.com/oshi/oshi/">See here</a>
     */
    private static String getLinuxProcessorName() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("/proc/cpuinfo"), StandardCharsets.UTF_8);
        Pattern whitespaceColonWhitespace = Pattern.compile("\\s+:\\s"); // From ParseUtil
        for (String line : lines) {
            String[] splitLine = whitespaceColonWhitespace.split(line);
            if ("model name".equals(splitLine[0]) || "Processor".equals(splitLine[0])) {
                return splitLine[1];
            }
        }
        return "unknown";
    }

    /**
     * <a href="https://stackoverflow.com/a/6327663">See here</a>
     */
    private static String getWindowsProcessorName() throws Exception {
        final String cpuNameCmd = "reg query \"HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0\" /v ProcessorNameString";
        final String regstrToken = "REG_SZ";

        Process process = Runtime.getRuntime().exec(cpuNameCmd);
        process.waitFor();
        InputStream is = process.getInputStream();

        StringBuilder sb = new StringBuilder();
        while (is.available() != 0) {
            sb.append((char) is.read());
        }

        String result = sb.toString();
        int p = result.indexOf(regstrToken);

        if (p == -1) {
            return "unknown";
        }

        return result.substring(p + regstrToken.length()).trim();
    }

    private CpuUtils() {
    }
}
