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

package org.geysermc.geyser.registry.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Value;
import org.geysermc.geyser.api.packs.ResourcePack;
import org.geysermc.geyser.api.packs.ResourcePackManifest;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

@Data
public class ResourcePackMapping implements ResourcePack {

    private byte[] sha256;
    private Path path;
    private ResourcePackManifest manifest;
    private ResourcePackManifest.Version version;
    private String contentKey;

    private File file;

    public ResourcePackMapping(byte[] sha256) {
        this.sha256 = sha256;
    }

    @Override
    public byte[] sha256() {
        return sha256;
    }

    @Override
    public Path path() {
        return path;
    }

    public long getLength() {
        return new File(path.toString()).length();
    }
    @Override
    public ResourcePackManifest manifest() {
        return manifest;
    }

    @Override
    public ResourcePackManifest.Version version() {
        return version;
    }

    @Override
    public String contentKey() {
        return contentKey;
    }

    public static class ResourcePackManifestMapping implements ResourcePackManifest {

        @JsonProperty("format_version")
        private int formatVersion;
        private Header header;
        private Collection<Module> modules;
        protected Collection<Dependency> dependencies;

        @Override
        public Integer formatVersion() {
            return formatVersion;
        }

        @Override
        public Header header() {
            return header;
        }

        @Override
        public Collection<Module> modules() {
            return modules;
        }

        @Override
        public Collection<Dependency> dependencies() {
            return dependencies;
        }
    }

    public class HeaderImpl implements ResourcePackManifest.Header {



        @Override
        public String description() {
            return null;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public UUID uuid() {
            return null;
        }

        @Override
        public int[] version() {
            return new int[0];
        }

        @Override
        public int[] minimumSupportedMinecraftVersion() {
            return new int[0];
        }

        @Override
        public String getVersionString() {
            return null;
        }
    }

    public class ResourcePackModuleImpl implements ResourcePackManifest.Module {

        @Override
        public String description() {
            return null;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public UUID uuid() {
            return null;
        }

        @Override
        public int[] version() {
            return new int[0];
        }
    }

    public class ResourcePackDependencyImpl implements ResourcePackManifest.Dependency {

        @Override
        public UUID uuid() {
            return null;
        }

        @Override
        public int[] version() {
            return new int[0];
        }
    }

    @Value
    public static class ResourcePackVersionImpl implements ResourcePackManifest.Version {
        private final int major;
        private final int minor;
        private final int patch;

        public static Version fromString(String ver) {
            String[] split = ver.replace(']', ' ')
                    .replace('[', ' ')
                    .replaceAll(" ", "").split(",");

            return new Version(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        }

        public static Version fromArray(int[] ver) {
            return new Version(ver[0], ver[1], ver[2]);
        }

        private Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
        @Override
        public int major() {
            return major;
        }

        @Override
        public int minor() {
            return minor;
        }

        @Override
        public int patch() {
            return patch;
        }
    }
}
