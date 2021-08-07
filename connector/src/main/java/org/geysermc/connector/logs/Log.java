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

package org.geysermc.connector.logs;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class Log {


    /**
     * pattern for IPv4 addresses
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile("(?:[1-2]?[0-9]{1,2}\\.){3}[1-2]?[0-9]{1,2}");
    /**
     * pattern for IPv6 addresses
     */
    private static final Pattern IPV6_PATTERN = Pattern.compile("(?:[0-9a-f]{0,4}:){7}[0-9a-f]{0,4}%", Pattern.CASE_INSENSITIVE);
    /**
     * log content
     */
    private String content;

    /**
     * create a new log
     *
     * @param dir  logs directory
     * @param file log file name
     * @throws IOException errors loading log
     */
    public Log(String dir, String file) throws IOException {
        File logs = new File(dir);
        File log = new File(logs, file);

        if (!log.getParentFile().equals(logs) || !file.matches(".*\\.log(\\.gz)?")) throw new FileNotFoundException();

        InputStream in = new FileInputStream(log);
        if (file.endsWith(".gz")) {
            in = new GZIPInputStream(in);
        }

        this.content = Util.inputStreamToString(in);
        this.filter();
    }

    /**
     * remove IP addresses
     */
    private void filter() {
        filterIPv4();
        filterIPv6();
    }

    /**
     * remove IPv4 addresses
     */
    private void filterIPv4() {
        Matcher matcher = IPV4_PATTERN.matcher(this.content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group().replaceAll("[0-9]", "*"));
        }
        matcher.appendTail(sb);
        this.content = sb.toString();
    }

    /**
     * remove IPv6 addresses
     */
    private void filterIPv6() {
        Matcher matcher = IPV6_PATTERN.matcher(this.content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group().replaceAll("[0-9a-fA-F]", "*"));
        }
        matcher.appendTail(sb);
        this.content = sb.toString();
    }

    /**
     * @return log content
     */
    public String getContent() {
        return content;
    }
}