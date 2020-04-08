package org.geysermc.connector.utils;

import org.geysermc.connector.GeyserConnector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class WebUtils {

    public static String getBody(String reqURL) {
        URL url = null;
        try {
            url = new URL(reqURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
                content.append("\n");
            }

            in.close();
            con.disconnect();

            return content.toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
