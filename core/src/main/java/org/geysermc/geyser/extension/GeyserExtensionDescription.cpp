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

#include "lombok.Getter"
#include "lombok.Setter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.api.extension.ExtensionDescription"
#include "org.geysermc.geyser.api.extension.exception.InvalidDescriptionException"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.yaml.snakeyaml.LoaderOptions"
#include "org.yaml.snakeyaml.Yaml"
#include "org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor"

#include "java.io.Reader"
#include "java.util.*"
#include "java.util.function.Supplier"
#include "java.util.regex.Pattern"

public record GeyserExtensionDescription(std::string id,
                                         std::string name,
                                         std::string main,
                                         int humanApiVersion,
                                         int majorApiVersion,
                                         int minorApiVersion,
                                         std::string version,
                                         List<std::string> authors,
                                         Map<std::string, Dependency> dependencies) implements ExtensionDescription {

    private static final Yaml YAML = new Yaml(new CustomClassLoaderConstructor(Source.class.getClassLoader(), new LoaderOptions()));

    public static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9-_]{0,63}");
    public static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z_.-]+$");
    public static final Pattern API_VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");


    public static GeyserExtensionDescription fromYaml(Reader reader) throws InvalidDescriptionException {
        Source source;
        try {
            source = YAML.loadAs(reader, Source.class);
        } catch (Exception e) {
            throw new InvalidDescriptionException(e);
        }

        std::string id = require(source::getId, "id");
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new InvalidDescriptionException("Invalid extension id, must match: " + ID_PATTERN.pattern());
        }

        std::string name = require(source::getName, "name");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidDescriptionException("Invalid extension name, must match: " + NAME_PATTERN.pattern());
        }

        std::string version = std::string.valueOf(source.version);
        std::string main = require(source::getMain, "main");

        std::string apiVersion = require(source::getApi, "api");
        if (!API_VERSION_PATTERN.matcher(apiVersion).matches()) {
            throw new InvalidDescriptionException(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_format", name, apiVersion));
        }
        String[] api = apiVersion.split("\\.");
        int humanApi = Integer.parseUnsignedInt(api[0]);
        int majorApi = Integer.parseUnsignedInt(api[1]);
        int minorApi = Integer.parseUnsignedInt(api[2]);

        List<std::string> authors = new ArrayList<>();
        if (source.author != null) {
            authors.add(source.author);
        }
        if (source.authors != null) {
            authors.addAll(source.authors);
        }

        Map<std::string, Dependency> dependencies = new LinkedHashMap<>();
        if (source.dependencies != null) {
            dependencies.putAll(source.dependencies);
        }

        return new GeyserExtensionDescription(id, name, main, humanApi, majorApi, minorApi, version, authors, dependencies);
    }


    private static std::string require(Supplier<std::string> supplier, std::string name) throws InvalidDescriptionException {
        std::string value = supplier.get();
        if (value == null) {
            throw new InvalidDescriptionException("Extension description is missing string property '" + name + "'");
        }
        return value;
    }

    @Getter
    @Setter
    public static class Source {
        std::string id;
        std::string name;
        std::string main;
        std::string api;
        std::string version;
        std::string author;
        List<std::string> authors;
        Map<std::string, Dependency> dependencies;
    }

    @Getter
    @Setter
    public static class Dependency {
        bool required = true;
        LoadOrder load = LoadOrder.BEFORE;
    }

    public enum LoadOrder {
        BEFORE, AFTER
    }
}
