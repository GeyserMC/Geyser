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

import org.geysermc.geyser.extension.exception.InvalidDescriptionException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

public class ExtensionDescription {
    private String name;
    private String main;
    private List<String> api;
    private String version;
    private final List<String> authors = new ArrayList<>();

    public ExtensionDescription(String yamlString) throws InvalidDescriptionException {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);
        this.loadMap(yaml.loadAs(yamlString, LinkedHashMap.class));
    }

    private void loadMap(Map<String, Object> yamlMap) throws InvalidDescriptionException {
        this.name = ((String) yamlMap.get("name")).replaceAll("[^A-Za-z0-9 _.-]", "");
        if (this.name.equals("")) {
            throw new InvalidDescriptionException("Invalid extension name");
        }
        this.name = this.name.replace(" ", "_");
        this.version = String.valueOf(yamlMap.get("version"));
        this.main = (String) yamlMap.get("main");

        Object api = yamlMap.get("api");
        if (api instanceof List) {
            this.api = (List<String>) api;
        } else {
            List<String> list = new ArrayList<>();
            list.add((String) api);
            this.api = list;
        }

        if (yamlMap.containsKey("author")) {
            this.authors.add((String) yamlMap.get("author"));
        }

        if (yamlMap.containsKey("authors")) {
            this.authors.addAll((Collection<? extends String>) yamlMap.get("authors"));
        }
    }

    public String getName() {
        return this.name;
    }

    public String getMain() {
        return this.main;
    }

    public List<String> getAPIVersions() {
        return api;
    }

    public String getVersion() {
        return this.version;
    }

    public List<String> getAuthors() {
        return this.authors;
    }
}
