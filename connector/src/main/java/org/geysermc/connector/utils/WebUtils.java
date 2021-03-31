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

package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.connector.GeyserConnector;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class WebUtils {

    /**
     * Makes a web request to the given URL and returns the body as a string
     *
     * @param reqURL URL to fetch
     * @return Body contents or error message if the request fails
     */
    public static String getBody(String reqURL) {
        URL url = null;
        try {
            url = new URL(reqURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Geyser-" + GeyserConnector.getInstance().getPlatformType().toString() + "/" + GeyserConnector.VERSION); // Otherwise Java 8 fails on checking updates

            return connectionToString(con);
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * Makes a web request to the given URL and returns the body as a {@link JsonNode}.
     *
     * @param reqURL URL to fetch
     * @return the response as JSON
     */
    public static JsonNode getJson(String reqURL) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(reqURL).openConnection();
        con.setRequestProperty("User-Agent", "Geyser-" + GeyserConnector.getInstance().getPlatformType().toString() + "/" + GeyserConnector.VERSION);
        return GeyserConnector.JSON_MAPPER.readTree(con.getInputStream());
    }

    /**
     * Downloads a file from the given URL and saves it to disk
     *
     * @param reqURL File to fetch
     * @param fileLocation Location to save on disk
     */
    public static void downloadFile(String reqURL, String fileLocation) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(reqURL).openConnection();
            con.setRequestProperty("User-Agent", "Geyser-" + GeyserConnector.getInstance().getPlatformType().toString() + "/" + GeyserConnector.VERSION);
            InputStream in = con.getInputStream();
            Files.copy(in, Paths.get(fileLocation), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new AssertionError("Unable to download and save file: " + fileLocation + " (" + reqURL + ")", e);
        }
    }

    public static String post(String reqURL, String postContent) throws IOException {
        URL url = new URL(reqURL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "text/plain");
        con.setRequestProperty("User-Agent", "Geyser-" + GeyserConnector.getInstance().getPlatformType().toString() + "/" + GeyserConnector.VERSION);
        con.setDoOutput(true);

        OutputStream out = con.getOutputStream();
        out.write(postContent.getBytes(StandardCharsets.UTF_8));
        out.close();

        return connectionToString(con);
    }

    /**
     * Get the string output from the passed {@link HttpURLConnection}
     *
     * @param con The connection to get the string from
     * @return The body of the returned page
     * @throws IOException
     */
    private static String connectionToString(HttpURLConnection con) throws IOException {
        // Send the request (we dont use this but its required for getErrorStream() to work)
        con.getResponseCode();

        // Read the error message if there is one if not just read normally
        InputStream inputStream = con.getErrorStream();
        if (inputStream == null) {
            inputStream = con.getInputStream();
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
                content.append("\n");
            }

            con.disconnect();
        }

        return content.toString();
    }
}
