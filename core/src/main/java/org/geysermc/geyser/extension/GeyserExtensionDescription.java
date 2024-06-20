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

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.extension.ExtensionDescription;
import org.geysermc.geyser.api.extension.exception.InvalidDescriptionException;
import org.geysermc.geyser.text.GeyserLocale;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.Reader;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public record GeyserExtensionDescription(@NonNull String id,
                                         @NonNull String name,
                                         @NonNull String main,
                                         int majorApiVersion,
                                         int minorApiVersion,
                                         int patchApiVersion,
                                         @NonNull String version,
                                         @NonNull List<String> authors) implements ExtensionDescription {

    private static final Yaml YAML = new Yaml(new CustomClassLoaderConstructor(Source.class.getClassLoader(), new LoaderOptions()));

    public static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9-_]{0,63}");
    public static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z_.-]+$");
    public static final Pattern API_VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    @NonNull
    public static GeyserExtensionDescription fromYaml(Reader reader) throws InvalidDescriptionException {
        Source source;
        try {
            source = YAML.loadAs(reader, Source.class);
        } catch (Exception e) {
            throw new InvalidDescriptionException(e);
        }

        String id = require(source::getId, "id");
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new InvalidDescriptionException("Invalid extension id, must match: " + ID_PATTERN.pattern());
        }

        String name = require(source::getName, "name");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidDescriptionException("Invalid extension name, must match: " + NAME_PATTERN.pattern());
        }

        String version = String.valueOf(source.version);
        String main = require(source::getMain, "main");

        String apiVersion = require(source::getApi, "api");
        if (!API_VERSION_PATTERN.matcher(apiVersion).matches()) {
            throw new InvalidDescriptionException(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_format", name, apiVersion));
        }
        String[] api = apiVersion.split("\\.");
        int majorApi = Integer.parseUnsignedInt(api[0]);
        int minorApi = Integer.parseUnsignedInt(api[1]);
        int patchApi = Integer.parseUnsignedInt(api[2]);

        List<String> authors = new ArrayList<>();
        if (source.author != null) {
            authors.add(source.author);
        }
        if (source.authors != null) {
            authors.addAll(source.authors);
        }

        return new GeyserExtensionDescription(id, name, main, majorApi, minorApi, patchApi, version, authors);
    }

    @NonNull
    private static String require(Supplier<String> supplier, String name) throws InvalidDescriptionException {
        String value = supplier.get();
        if (value == null) {
            throw new InvalidDescriptionException("Extension description is missing string property '" + name + "'");
        }
        return value;
    }

    @Getter
    @Setter
    public static class Source {
        String id;
        String name;
        String main;
        String api;
        String version;
        String author;
        List<String> authors;
    }
}
