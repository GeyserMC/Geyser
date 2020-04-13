package org.geysermc.connector.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * author: NukkitX
 * Nukkit Project
 */
@Getter
@EqualsAndHashCode
public class ResourcePackManifest {
    @JsonProperty("format_version")
    private Integer formatVersion;
    private Header header;
    private Collection<Module> modules;
    protected Collection<Dependency> dependencies;

    public Collection<Module> getModules() {
        return Collections.unmodifiableCollection(modules);
    }

    @Getter
    @ToString
    public static class Header {
        private String description;
        private String name;
        private UUID uuid;
        private int[] version;
        @JsonProperty("min_engine_version")
        private int[] minimumSupportedMinecraftVersion;
    }

    @Getter
    @ToString
    public static class Module {
        private String description;
        private String name;
        private UUID uuid;
        private int[] version;
    }

    @Getter
    @ToString
    public static class Dependency {
        private UUID uuid;
        private int[] version;
    }

    @Value
    public static class Version {
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
    }
}

