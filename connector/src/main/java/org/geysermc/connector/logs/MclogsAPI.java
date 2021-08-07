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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MclogsAPI {


    public static String mcversion = "unknown";
    public static String userAgent = "unknown";
    public static String version = "unknown";
    private static String apiHost = "api.mclo.gs";
    private static String protocol = "https";

    /**
     * share a log to the mclogs API
     *
     * @param dir  logs directory
     * @param file log file name
     * @return mclogs response
     * @throws IOException error reading/sharing file
     */
    public static APIResponse share(String dir, String file) throws IOException {
        Log log = new Log(dir, file);

        //connect to api
        URL url = new URL(protocol + "://" + apiHost + "/1/log");
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);

        //convert log to application/x-www-form-urlencoded
        String content = "content=" + URLEncoder.encode(log.getContent(), StandardCharsets.UTF_8.toString());
        byte[] out = content.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        //send log to api
        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        http.setRequestProperty("User-Agent", userAgent + "/" + version + "/" + mcversion);
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(out);
        }

        //handle response
        return APIResponse.parse(Util.inputStreamToString(http.getInputStream()));
    }

    /**
     * list logs in a directory
     *
     * @param rundir server/client directory
     * @return log file names
     */
    public static String[] listLogs(String rundir) {
        File logdir = new File(rundir, "logs");

        String[] files = logdir.list();
        if (files == null)
            files = new String[0];

        return Arrays.stream(files)
                .filter(file -> file.endsWith(".log") || file.endsWith(".log.gz"))
                .toArray(String[]::new);
    }

    /**
     * @return api host URL
     */
    public static String getApiHost() {
        return apiHost;
    }

    /**
     * @param apiHost api host url
     */
    public static void setApiHost(String apiHost) {
        if (apiHost != null && apiHost.length() > 0) MclogsAPI.apiHost = apiHost;
    }

    /**
     * @return protocol
     */
    public static String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol protocol
     */
    public static void setProtocol(String protocol) {
        if (protocol == null) return;
        switch (protocol) {
            case "http":
            case "https":
                MclogsAPI.protocol = protocol;
                break;
        }
    }
}
