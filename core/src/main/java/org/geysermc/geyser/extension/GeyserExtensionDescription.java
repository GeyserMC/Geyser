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

import org.geysermc.geyser.api.extension.ExtensionDescription;
import org.geysermc.geyser.api.extension.exception.InvalidDescriptionException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.Reader;
import java.util.*;

public record GeyserExtensionDescription(String name, String main, String apiVersion, String version, List<String> authors) implements ExtensionDescription {
    @SuppressWarnings("unchecked")
    public static GeyserExtensionDescription fromYaml(Reader reader) throws InvalidDescriptionException {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(dumperOptions);
        Map<String, Object> yamlMap = yaml.loadAs(reader, LinkedHashMap.class);

        String name = ((String) yamlMap.get("name")).replaceAll("[^A-Za-z0-9 _.-]", "");
        if (name.isBlank()) {
            throw new InvalidDescriptionException("Invalid extension name, cannot be empty");
        }

        name = name.replace(" ", "_");
        String version = String.valueOf(yamlMap.get("version"));
        String main = (String) yamlMap.get("main");
        String apiVersion;

        Object api = yamlMap.get("api");
        if (api instanceof String) {
            apiVersion = (String) api;
        } else {
            throw new InvalidDescriptionException("Invalid api version format, should be a string: major.minor.patch");
        }

        List<String> authors = new ArrayList<>();
        if (yamlMap.containsKey("author")) {
            authors.add((String) yamlMap.get("author"));
        }

        if (yamlMap.containsKey("authors")) {
            try {
                authors.addAll((Collection<? extends String>) yamlMap.get("authors"));
            } catch (Exception e) {
                throw new InvalidDescriptionException("Invalid authors format, should be a list of strings", e);
            }
        }

        return new GeyserExtensionDescription(name, main, apiVersion, version, authors);
    }
}
