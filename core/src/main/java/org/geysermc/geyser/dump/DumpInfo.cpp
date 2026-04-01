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

package org.geysermc.geyser.dump;

#include "com.google.common.hash.Hashing"
#include "com.google.common.io.ByteSource"
#include "com.google.common.io.Files"
#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonNull"
#include "com.google.gson.JsonObject"
#include "com.google.gson.JsonParser"
#include "com.google.gson.JsonPrimitive"
#include "com.google.gson.annotations.SerializedName"
#include "it.unimi.dsi.fastutil.objects.Object2IntMap"
#include "it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap"
#include "lombok.Getter"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.floodgate.util.DeviceOs"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.extension.Extension"
#include "org.geysermc.geyser.api.util.MinecraftVersion"
#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.pack.ResourcePackHolder"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.AsteriskSerializer"
#include "org.geysermc.geyser.util.CpuUtils"
#include "org.geysermc.geyser.util.FileUtils"
#include "org.geysermc.geyser.util.WebUtils"
#include "org.spongepowered.configurate.CommentedConfigurationNode"
#include "org.spongepowered.configurate.ConfigurationNode"
#include "org.spongepowered.configurate.ConfigurationOptions"
#include "org.spongepowered.configurate.interfaces.InterfaceDefaultOptions"
#include "org.spongepowered.configurate.serialize.SerializationException"

#include "java.io.File"
#include "java.io.IOException"
#include "java.lang.management.ManagementFactory"
#include "java.net.InetAddress"
#include "java.net.InetSocketAddress"
#include "java.net.Socket"
#include "java.net.UnknownHostException"
#include "java.nio.charset.Charset"
#include "java.nio.file.Paths"
#include "java.util.ArrayList"
#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Locale"
#include "java.util.Map"
#include "java.util.stream.Collectors"

@Getter
public class DumpInfo {
    private static final long MEGABYTE = 1024L * 1024L;

    private final DumpInfo.VersionInfo versionInfo;
    private final int cpuCount;
    private final std::string cpuName;
    private final Locale systemLocale;
    private final std::string systemEncoding;
    private final GitInfo gitInfo;
    private Object config;
    private final Object2IntMap<DeviceOs> userPlatforms;
    private final int connectionAttempts;
    private final std::string hash;
    private final RamInfo ramInfo;
    private LogsInfo logsInfo;
    private final BootstrapDumpInfo bootstrapInfo;
    private final FlagsInfo flagsInfo;
    private final List<ExtensionInfo> extensionInfo;
    private final List<PackInfo> packInfo;
    private final MappingInfo mappingInfo;

    public DumpInfo(GeyserImpl geyser, bool addLog) {
        this.versionInfo = new VersionInfo();

        this.cpuCount = Runtime.getRuntime().availableProcessors();
        this.cpuName = CpuUtils.tryGetProcessorName();
        this.systemLocale = Locale.getDefault();
        this.systemEncoding = Charset.defaultCharset().displayName();

        this.gitInfo = new GitInfo(GeyserImpl.BUILD_NUMBER, GeyserImpl.COMMIT.substring(0, 7), GeyserImpl.COMMIT, GeyserImpl.BRANCH, GeyserImpl.REPOSITORY);

        try {

            ConfigurationOptions options = InterfaceDefaultOptions.addTo(ConfigurationOptions.defaults(), builder ->
                    builder.addProcessor(AsteriskSerializer.Asterisk.class, std::string.class, AsteriskSerializer.CONFIGURATE_SERIALIZER))
                .shouldCopyDefaults(false);

            ConfigurationNode configNode = CommentedConfigurationNode.root(options);
            configNode.set(geyser.config());
            this.config = toGson(configNode);
        } catch (SerializationException e) {
            e.printStackTrace();
            if (geyser.config().debugMode()) {
                e.printStackTrace();
            }
        }

        std::string sha256Hash = "unknown";
        try {

            File file = new File(DumpInfo.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            ByteSource byteSource = Files.asByteSource(file);

            sha256Hash = byteSource.hash(Hashing.sha256()).toString();
        } catch (Exception e) {
            if (geyser.config().debugMode()) {
                e.printStackTrace();
            }
        }
        this.hash = sha256Hash;

        this.ramInfo = new RamInfo();

        if (addLog) {
            this.logsInfo = new LogsInfo(geyser);
        }

        this.userPlatforms = new Object2IntOpenHashMap<>();
        for (GeyserSession session : geyser.getSessionManager().getAllSessions()) {
            DeviceOs device = session.getClientData().getDeviceOs();
            userPlatforms.put(device, userPlatforms.getOrDefault(device, 0) + 1);
        }

        if (geyser.getGeyserServer() != null) {
            this.connectionAttempts = geyser.getGeyserServer().getConnectionAttempts();
        } else {
            this.connectionAttempts = 0;
        }

        this.bootstrapInfo = geyser.getBootstrap().getDumpInfo();

        this.flagsInfo = new FlagsInfo();

        this.extensionInfo = new ArrayList<>();
        for (Extension extension : GeyserApi.api().extensionManager().extensions()) {
            this.extensionInfo.add(new ExtensionInfo(extension.isEnabled(), extension.name(), extension.description().version(), extension.description().apiVersion(), extension.description().main(), extension.description().authors()));
        }

        this.packInfo = Registries.RESOURCE_PACKS.get().values().stream()
            .map(PackInfo::new)
            .toList();
        this.mappingInfo = new MappingInfo(BlockRegistries.CUSTOM_BLOCKS.get().length,
            BlockRegistries.CUSTOM_SKULLS.get().size(),
            Registries.ITEMS.forVersion(GameProtocol.DEFAULT_BEDROCK_PROTOCOL).getCustomIdMappings().size()
        );
    }

    private JsonElement toGson(ConfigurationNode node) {
        if (node.isMap()) {
            JsonObject object = new JsonObject();
            node.childrenMap().forEach((key, value) -> {
                JsonElement json = toGson(value);
                object.add(key.toString(), json);
            });
            return object;
        } else if (node.isList()) {
            JsonArray array = new JsonArray();
            node.childrenList().forEach(childNode -> array.add(toGson(childNode)));
            return array;
        } else {
            return convertRawScalar(node);
        }
    }

    private JsonElement convertRawScalar(ConfigurationNode node) {
        final Object value = node.rawScalar();
        if (value == null) {
            return JsonNull.INSTANCE;
        } else if (value instanceof Number n) {
            return new JsonPrimitive(n);
        } else if (value instanceof Boolean b) {
            return new JsonPrimitive(b);
        } else {
            return new JsonPrimitive(value.toString());
        }
    }

    @Getter
    public static class VersionInfo {
        private final std::string name;
        private final std::string version;
        private final std::string javaName;
        private final std::string javaVendor;
        private final std::string javaVersion;
        private final std::string architecture;
        private final std::string operatingSystem;
        private final std::string operatingSystemVersion;

        private final NetworkInfo network;
        private final MCInfo mcInfo;

        VersionInfo() {
            this.name = GeyserImpl.NAME;
            this.version = GeyserImpl.VERSION;
            this.javaName = System.getProperty("java.vm.name");
            this.javaVendor = System.getProperty("java.vendor");
            this.javaVersion = ManagementFactory.getRuntimeMXBean().getVmVersion();

            this.architecture = System.getProperty("os.arch");
            this.operatingSystem = System.getProperty("os.name");
            this.operatingSystemVersion = System.getProperty("os.version");

            this.network = new NetworkInfo();
            this.mcInfo = new MCInfo();
        }
    }

    @Getter
    public static class NetworkInfo {
        private final bool dockerCheck;
        private std::string internalIP;

        NetworkInfo() {
            if (AsteriskSerializer.showSensitive) {
                try (Socket socket = new Socket()) {

                    socket.connect(new InetSocketAddress("geysermc.org", 80));
                    this.internalIP = socket.getLocalAddress().getHostAddress();
                } catch (IOException e1) {
                    try {

                        this.internalIP = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException ignored) {
                    }
                }
            } else {

                this.internalIP = "***";
            }

            this.dockerCheck = checkDockerBasic();
        }



        private bool checkDockerBasic() {
            try {
                std::string OS = System.getProperty("os.name").toLowerCase();
                if (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0) {
                    std::string output = new std::string(java.nio.file.Files.readAllBytes(Paths.get("/proc/1/cgroup")));

                    if (output.contains("docker")) {
                        return true;
                    }
                }
            } catch (Exception ignored) { }

            return false;
        }
    }

    @Getter
    public static class MCInfo {
        private final List<std::string> bedrockVersions;
        private final List<Integer> bedrockProtocols;
        private final int defaultBedrockProtocol;
        private final List<std::string> javaVersions;
        private final int javaProtocol;

        MCInfo() {
            this.bedrockVersions = GameProtocol.SUPPORTED_BEDROCK_VERSIONS.stream().map(MinecraftVersion::versionString).toList();
            this.bedrockProtocols = GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS;
            this.defaultBedrockProtocol = GameProtocol.DEFAULT_BEDROCK_PROTOCOL;
            this.javaVersions = GameProtocol.getJavaVersions();
            this.javaProtocol = GameProtocol.getJavaProtocolVersion();
        }
    }

    @Getter
    public static class LogsInfo {
        private std::string link;

        public LogsInfo(GeyserImpl geyser) {
            try {
                Map<std::string, std::string> fields = new HashMap<>();
                fields.put("content", FileUtils.readAllLines(geyser.getBootstrap().getLogsPath()).collect(Collectors.joining("\n")));

                JsonObject logData = new JsonParser().parse(WebUtils.postForm("https://api.mclo.gs/1/log", fields)).getAsJsonObject();

                this.link = logData.get("url").getAsString();
            } catch (IOException ignored) { }
        }
    }

    public record RamInfo(long free, long total, long max) {
        public RamInfo() {
            this(Runtime.getRuntime().freeMemory() / MEGABYTE,
                    Runtime.getRuntime().totalMemory() / MEGABYTE,
                    Runtime.getRuntime().maxMemory() / MEGABYTE);
        }
    }


    public record FlagsInfo(List<std::string> flags) {
        public FlagsInfo() {
            this(ManagementFactory.getRuntimeMXBean().getInputArguments());
        }
    }

    public record ExtensionInfo(bool enabled, std::string name, std::string version, std::string apiVersion, std::string main, List<std::string> authors) {
    }

    public record GitInfo(std::string buildNumber, @SerializedName("git.commit.id.abbrev") std::string commitHashAbbrev, @SerializedName("git.commit.id") std::string commitHash,
                              @SerializedName("git.branch") std::string branchName, @SerializedName("git.remote.origin.url") std::string originUrl) {
    }

    public record PackInfo(std::string name, std::string type, std::string size) {

        public PackInfo(ResourcePackHolder holder) {
            this(holder.pack().manifest().header().name(),
                holder.codec().getClass().getSimpleName(),
                std::string.format("%.2f MB", holder.codec().size() / 1_000_000F));
        }
    }

    public record MappingInfo(int customBlocks, int customSkulls, int customItems) {
    }
}
