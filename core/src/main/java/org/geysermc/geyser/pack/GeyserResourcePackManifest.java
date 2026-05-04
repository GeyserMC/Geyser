/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.pack.ResourcePackManifest;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public record GeyserResourcePackManifest(
    @SerializedName("format_version") int formatVersion,
    Header header,
    Collection<Module> modules,
    Collection<Dependency> dependencies,
    Collection<Subpack> subpacks,
    Collection<Setting> settings
) implements ResourcePackManifest {
    public GeyserResourcePackManifest(int formatVersion,
                                      Header header,
                                      Collection<Module> modules,
                                      Collection<Dependency> dependencies,
                                      Collection<Subpack> subpacks,
                                      Collection<Setting> settings) {
        this.formatVersion = formatVersion;
        this.header = header;
        this.modules = ensureNonNull(modules);
        this.dependencies = ensureNonNull(dependencies);
        this.subpacks = ensureNonNull(subpacks);
        this.settings = ensureNonNull(settings);
    }

    public record Header(UUID uuid, Version version, String name, String description, @SerializedName("min_engine_version") Version minimumSupportedMinecraftVersion) implements ResourcePackManifest.Header { }

    public record Module(UUID uuid, Version version, String type, String description) implements ResourcePackManifest.Module { }

    public record Dependency(UUID uuid, Version version) implements ResourcePackManifest.Dependency { }

    public record Subpack(@SerializedName("folder_name") String folderName, String name, @SerializedName("memory_tier") Float memoryTier) implements ResourcePackManifest.Subpack { }

    public record Setting(String type, String text) implements ResourcePackManifest.Setting { }

    static <T> Collection<T> ensureNonNull(Collection<T> collection) {
        if (collection == null) return Collections.emptyList();
        return Collections.unmodifiableCollection(collection);
    }

    @JsonAdapter(value = Version.VersionDeserializer.class)
    public record Version(int major, int minor, int patch) implements ResourcePackManifest.Version {

        @Override
        public @NonNull String toString() {
            return major + "." + minor + "." + patch;
        }

        public static class VersionDeserializer implements JsonDeserializer<Version> {
            @Override
            public Version deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonArray()) {
                    JsonArray array = json.getAsJsonArray();
                    return new Version(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
                }

                if (json.isJsonPrimitive()) {
                    String version = json.getAsJsonPrimitive().getAsString();
                    String[] parts = version.split("\\.");

                    int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
                    int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                    int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                    return new Version(major, minor, patch);
                }

                throw new JsonParseException("Unsure how to convert " + json + " to version");
            }
        }
    }
}

