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

package org.geysermc.geyser.extension;

import org.geysermc.geyser.api.extension.exception.InvalidDescriptionException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class GeyserExtensionDescription implements org.geysermc.geyser.api.extension.ExtensionDescription {
    private String name;
    private String main;
    private String api;
    private String version;
    private final List<String> authors = new ArrayList<>();

    public GeyserExtensionDescription(InputStream inputStream) throws InvalidDescriptionException {
        try {
            InputStreamReader reader = new InputStreamReader(inputStream);
            StringBuilder builder = new StringBuilder();
            String temp;
            BufferedReader bufferedReader = new BufferedReader(reader);
            temp = bufferedReader.readLine();
            while (temp != null) {
                if (builder.length() != 0) {
                    builder.append("\n");
                }
                builder.append(temp);
                temp = bufferedReader.readLine();
            }

            this.loadString(builder.toString());
        } catch (IOException e) {
            throw new InvalidDescriptionException(e);
        }
    }

    public GeyserExtensionDescription(String yamlString) throws InvalidDescriptionException {
        this.loadString(yamlString);
    }

    private void loadString(String yamlString) throws InvalidDescriptionException {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);
        this.loadMap(yaml.loadAs(yamlString, LinkedHashMap.class));
    }

    private void loadMap(Map<String, Object> yamlMap) throws InvalidDescriptionException {
        this.name = ((String) yamlMap.get("name")).replaceAll("[^A-Za-z0-9 _.-]", "");
        if (this.name.equals("")) {
            throw new InvalidDescriptionException("Invalid extension name, cannot be empty");
        }
        this.name = this.name.replace(" ", "_");
        this.version = String.valueOf(yamlMap.get("version"));
        this.main = (String) yamlMap.get("main");

        Object api = yamlMap.get("api");
        if (api instanceof String) {
            this.api = (String) api;
        } else {
            this.api = "0.0.0";
            throw new InvalidDescriptionException("Invalid api version format, should be a string: major.minor.patch");
        }

        if (yamlMap.containsKey("author")) {
            this.authors.add((String) yamlMap.get("author"));
        }

        if (yamlMap.containsKey("authors")) {
            try {
                this.authors.addAll((Collection<? extends String>) yamlMap.get("authors"));
            } catch (Exception e) {
                throw new InvalidDescriptionException("Invalid authors format, should be a list of strings", e);
            }
        }
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String main() {
        return this.main;
    }

    @Override
    public String apiVersion() {
        return api;
    }

    @Override
    public String version() {
        return this.version;
    }

    @Override
    public List<String> authors() {
        return this.authors;
    }
}
