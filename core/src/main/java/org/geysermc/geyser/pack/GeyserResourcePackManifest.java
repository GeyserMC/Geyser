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

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.pack.ResourcePackManifest;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public record GeyserResourcePackManifest(@SerializedName("format_version") int formatVersion, Header header, Collection<Module> modules, Collection<Dependency> dependencies) implements ResourcePackManifest {

    public record Header(UUID uuid, Version version, String name, String description, @SerializedName("min_engine_version") Version minimumSupportedMinecraftVersion) implements ResourcePackManifest.Header { }

    public record Module(UUID uuid, Version version, String type, String description) implements ResourcePackManifest.Module { }

    public record Dependency(UUID uuid, Version version) implements ResourcePackManifest.Dependency { }

    public record Version(int major, int minor, int patch) implements ResourcePackManifest.Version {

        @Override
        public @NonNull String toString() {
            return major + "." + minor + "." + patch;
        }

        public static class VersionDeserializer extends TypeAdapter<Version> {
            @Override
            public void write(JsonWriter jsonWriter, Version version) throws IOException {
            }

            @Override
            public Version read(JsonReader jsonReader) throws IOException {
                jsonReader.beginArray();
                Version version = new Version(jsonReader.nextInt(), jsonReader.nextInt(), jsonReader.nextInt());
                jsonReader.endArray();
                return version;
            }
        }
    }
}

