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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import org.geysermc.geyser.api.packs.GeyserResourcePackManifest;

import java.util.Collection;
import java.util.UUID;

/**
 * author: NukkitX
 * Nukkit Project
 */

@Setter
@EqualsAndHashCode
public class ResourcePackManifest implements GeyserResourcePackManifest {
    @JsonProperty("format_version")
    private Integer formatVersion;
    private Header header;
    private Collection<GeyserResourcePackManifest.Module> modules;
    protected Collection<GeyserResourcePackManifest.Dependency> dependencies;

    @Override
    public Integer formatVersion() {
        return formatVersion;
    }

    @Override
    public GeyserResourcePackManifest.Header header() {
        return header;
    }

    @Override
    public Collection<GeyserResourcePackManifest.Module> modules() {
        return modules;
    }

    @Override
    public Collection<GeyserResourcePackManifest.Dependency> dependencies() {
        return dependencies;
    }

    @ToString
    public static class Header implements GeyserResourcePackManifest.Header {
        private String description;
        private String name;
        private UUID uuid;
        private int[] version;
        @JsonProperty("min_engine_version")
        private int[] minimumSupportedMinecraftVersion;

        @Override
        public String description() {
            return description;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public UUID uuid() {
            return uuid;
        }

        @Override
        public int[] version() {
            return version;
        }

        @Override
        public int[] minimumSupportedMinecraftVersion() {
            return minimumSupportedMinecraftVersion;
        }

        @Override
        public String versionString() {
            return version[0] + "." + version[1] + "." + version[2];
        }
    }

    @ToString
    public static class Module implements GeyserResourcePackManifest.Module{
        private String description;
        private String name;
        private UUID uuid;
        private int[] version;

        @Override
        public String description() {
            return description;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public UUID uuid() {
            return uuid;
        }

        @Override
        public int[] version() {
            return version;
        }
    }

    @ToString
    public static class Dependency implements GeyserResourcePackManifest.Dependency {
        private UUID uuid;
        private int[] version;

        @Override
        public UUID uuid() {
            return uuid;
        }

        @Override
        public int[] version() {
            return version;
        }
    }

    @Value
    public static class Version implements GeyserResourcePackManifest.Version{
        int major;
        int minor;
        int patch;

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

