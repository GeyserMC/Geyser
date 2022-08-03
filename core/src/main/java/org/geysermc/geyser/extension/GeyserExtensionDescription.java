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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.extension.ExtensionDescription;
import org.geysermc.geyser.api.extension.exception.InvalidDescriptionException;
import org.geysermc.geyser.text.GeyserLocale;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.util.*;
import java.util.regex.Pattern;

public record GeyserExtensionDescription(@NonNull String name,
                                         @NonNull String main,
                                         int majorApiVersion,
                                         int minorApiVersion,
                                         int patchApiVersion,
                                         @NonNull String version,
                                         @NonNull List<String> authors) implements ExtensionDescription {

    private static final Yaml YAML = new Yaml();
    public static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z_.-]*$");
    public static final Pattern API_VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    @NonNull
    @SuppressWarnings("unchecked")
    public static GeyserExtensionDescription fromYaml(Reader reader) throws InvalidDescriptionException {
        Map<String, Object> map;
        try {
            map = YAML.loadAs(reader, HashMap.class);
        } catch (Exception e) {
            throw new InvalidDescriptionException(e);
        }

        String name = require(map, "name");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidDescriptionException("Invalid extension name, must match: " + NAME_PATTERN.pattern());
        }
        String version = String.valueOf(map.get("version"));
        String main = require(map, "main");

        String apiVersion = require(map, "api");
        if (!API_VERSION_PATTERN.matcher(apiVersion).matches()) {
            throw new InvalidDescriptionException(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_format", name, apiVersion));
        }
        String[] api = apiVersion.split("\\.");
        int majorApi = Integer.parseUnsignedInt(api[0]);
        int minorApi = Integer.parseUnsignedInt(api[1]);
        int patchApi = Integer.parseUnsignedInt(api[2]);

        List<String> authors = new ArrayList<>();
        if (map.containsKey("author")) {
            authors.add(String.valueOf(map.get("author")));
        }
        if (map.containsKey("authors")) {
            try {
                authors.addAll((Collection<? extends String>) map.get("authors"));
            } catch (Exception e) {
                throw new InvalidDescriptionException("Invalid authors format, should be a list of strings", e);
            }
        }

        return new GeyserExtensionDescription(name, main, majorApi, minorApi, patchApi, version, authors);
    }

    @NonNull
    private static String require(Map<String, Object> desc, String key) throws InvalidDescriptionException {
        Object value = desc.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        throw new InvalidDescriptionException("Extension description is missing string property '" + key + "'");
    }
}
