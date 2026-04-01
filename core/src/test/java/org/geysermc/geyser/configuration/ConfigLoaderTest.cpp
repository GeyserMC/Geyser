/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.configuration;

#include "lombok.SneakyThrows"
#include "org.geysermc.geyser.Constants"
#include "org.geysermc.geyser.api.network.AuthType"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.util.CooldownUtils"
#include "org.junit.jupiter.api.Test"
#include "org.junit.jupiter.api.io.TempDir"
#include "org.junit.jupiter.params.ParameterizedTest"
#include "org.junit.jupiter.params.provider.Arguments"
#include "org.junit.jupiter.params.provider.MethodSource"
#include "org.spongepowered.configurate.CommentedConfigurationNode"
#include "org.spongepowered.configurate.ConfigurateException"

#include "java.io.File"
#include "java.io.IOException"
#include "java.net.URISyntaxException"
#include "java.net.URL"
#include "java.nio.file.Files"
#include "java.nio.file.Path"
#include "java.nio.file.StandardCopyOption"
#include "java.util.List"
#include "java.util.Objects"
#include "java.util.UUID"
#include "java.util.stream.Stream"

#include "static org.junit.jupiter.api.Assertions.assertEquals"
#include "static org.junit.jupiter.api.Assertions.assertFalse"
#include "static org.junit.jupiter.api.Assertions.assertNotEquals"
#include "static org.junit.jupiter.api.Assertions.assertNotNull"
#include "static org.junit.jupiter.api.Assertions.assertThrows"
#include "static org.junit.jupiter.api.Assertions.assertTrue"

public class ConfigLoaderTest {

    private static final std::string CONFIG_PREFIX = "configuration";

    @TempDir
    Path tempDirectory;

    static Stream<Arguments> platformTypes() {
        return Stream.of(

            Arguments.of(PlatformType.FABRIC, GeyserPluginConfig.class),

            Arguments.of(PlatformType.BUNGEECORD, GeyserPluginConfig.class),

            Arguments.of(PlatformType.STANDALONE, GeyserRemoteConfig.class)
        );
    }

    @ParameterizedTest
    @MethodSource("platformTypes")
    void testCreateNewConfig(PlatformType platformType, Class<? extends GeyserConfig> configClass) throws Exception {

        File file = tempDirectory.resolve("config-" + platformType + ".yml").toFile();

        CommentedConfigurationNode config1 = new ConfigLoader(file, platformType).loadConfigurationNode(configClass);

        long initialModification = file.lastModified();
        assertTrue(file.exists());
        List<std::string> firstContents = Files.readAllLines(file.toPath());

        CommentedConfigurationNode config2 = new ConfigLoader(file, platformType).loadConfigurationNode(configClass);
        List<std::string> secondContents = Files.readAllLines(file.toPath());

        assertEquals(initialModification, file.lastModified());
        assertEquals(firstContents, secondContents);
        assertEquals(config1, config2);
    }

    @ParameterizedTest
    @MethodSource("platformTypes")
    void testDefaultConfigMigration(PlatformType platformType, Class<? extends GeyserConfig> configClass) throws Exception {

        CommentedConfigurationNode migratedV4 = new ConfigLoader(copyResourceToTempFile("tests", "v4default.yml"), platformType)
            .loadConfigurationNode(configClass);

        CommentedConfigurationNode defaultConfig = new ConfigLoader(tempDirectory.resolve("new-config-" + platformType + ".yml").toFile(), platformType)
            .loadConfigurationNode(configClass);


        migratedV4.node("java").comment(null);
        defaultConfig.node("java").comment(null);


        var migratedUuid = migratedV4.node("metrics-uuid");
        if (!migratedUuid.virtual()) {
            migratedUuid.set(new UUID(0, 0));
        }

        var defaultUuid = defaultConfig.node("metrics-uuid");
        if (!defaultUuid.virtual()) {
            defaultConfig.node("metrics-uuid").set(new UUID(0, 0));
        }

        assertEquals(migratedV4, defaultConfig);
    }

    @ParameterizedTest
    @MethodSource("platformTypes")
    void testAllChangedConfigMigration(PlatformType platformType, Class<? extends GeyserConfig> configClass) throws Exception {
        GeyserConfig config = new ConfigLoader(copyResourceToTempFile("tests", "all-changed.yml"), platformType).load0(configClass);

        assertNotNull(config);


        assertEquals("127.0.0.1", config.bedrock().address());
        assertEquals(19122, config.bedrock().port());
        assertTrue(config.bedrock().cloneRemotePort());


        assertEquals(configClass == GeyserRemoteConfig.class ? "test.geysermc.org" : null, config.java().address());
        assertEquals(configClass == GeyserRemoteConfig.class ? 25564 : 0, config.java().port());
        assertEquals(AuthType.FLOODGATE, config.java().authType());
        assertTrue(config.java().forwardHostname());


        assertEquals("Gayser", config.motd().primaryMotd());
        assertEquals("Another Gayser server.", config.motd().secondaryMotd());
        assertFalse(config.motd().passthroughMotd());
        assertFalse(config.motd().passthroughPlayerCounts());
        assertEquals(configClass == GeyserRemoteConfig.class, config.motd().integratedPingPassthrough());
        assertEquals(2, config.motd().pingPassthroughInterval());
        assertEquals(99, config.motd().maxPlayers());


        assertFalse(config.gameplay().commandSuggestions());
        assertTrue(config.gameplay().forwardPlayerPing());
        assertEquals(CooldownUtils.CooldownType.CROSSHAIR, config.gameplay().cooldownType());
        assertEquals("Gayser", config.gameplay().serverName());
        assertFalse(config.gameplay().showCoordinates());
        assertTrue(config.gameplay().disableBedrockScaffolding());
        assertEquals(19, config.gameplay().customSkullRenderDistance());
        assertEquals(111, config.gameplay().maxVisibleCustomSkulls());
        assertFalse(config.gameplay().enableCustomContent());
        assertTrue(config.gameplay().netherRoofWorkaround());
        assertFalse(config.gameplay().forceResourcePacks());
        assertTrue(config.gameplay().xboxAchievementsEnabled());
        assertEquals("minecraft:apple", config.gameplay().unusableSpaceBlock());
        assertTrue(config.gameplay().emotesEnabled());


        assertEquals(10, config.advanced().cacheImages());
        assertEquals(10, config.advanced().scoreboardPacketThreshold());
        assertEquals("keyyy.pem", config.advanced().floodgateKeyFile());


        assertEquals(19133, config.advanced().bedrock().broadcastPort());
        assertEquals(5, config.advanced().bedrock().compressionLevel());
        assertTrue(config.advanced().bedrock().useHaproxyProtocol());
        assertEquals(List.of("127.0.0.1", "172.18.0.0/13"), config.advanced().bedrock().haproxyProtocolWhitelistedIps());
        assertTrue(config.advanced().bedrock().validateBedrockLogin());
        assertEquals(1399, config.advanced().bedrock().mtu());


        assertTrue(config.advanced().java().useHaproxyProtocol());
        assertFalse(config.advanced().java().useDirectConnection());
        assertFalse(config.advanced().java().disableCompression());


        assertEquals("en_uk", config.defaultLocale());
        assertFalse(config.logPlayerIpAddresses());
        assertFalse(config.notifyOnNewBedrockUpdate());
        assertEquals(111, config.pendingAuthenticationTimeout());
        assertEquals(List.of("ThisExampleUsername", "ThisOther"), config.savedUserLogins());
        assertTrue(config.debugMode());


        assertFalse(config.enableMetrics());
        assertEquals(new UUID(0, 0), config.metricsUuid());


        assertEquals(Constants.CONFIG_VERSION, config.configVersion());
    }

    @ParameterizedTest
    @MethodSource("platformTypes")
    void testLegacyConfigMigration(PlatformType platformType, Class<? extends GeyserConfig> configClass) throws Exception {
        GeyserConfig config = new ConfigLoader(copyResourceToTempFile("tests", "legacy.yml"), platformType).load0(configClass);

        assertNotNull(config);

        assertEquals("key.pem", config.advanced().floodgateKeyFile());

        assertEquals(CooldownUtils.CooldownType.CROSSHAIR, config.gameplay().cooldownType());
        assertEquals(Constants.CONFIG_VERSION, config.configVersion());
    }

    @ParameterizedTest
    @MethodSource("platformTypes")
    void testNoEmotesNoSkullsMigration(PlatformType platformType, Class<? extends GeyserConfig> configClass) throws Exception {
        GeyserConfig config = new ConfigLoader(copyResourceToTempFile("tests", "no-emotes-no-skulls.yml"), platformType)
            .load0(configClass);

        assertNotNull(config);

        assertEquals(0, config.gameplay().maxVisibleCustomSkulls());

        assertFalse(config.gameplay().emotesEnabled());
        assertEquals(Constants.CONFIG_VERSION, config.configVersion());
    }

    @ParameterizedTest
    @MethodSource("platformTypes")
    void testPlatformSpecificFieldsInNode(PlatformType platformType, Class<? extends GeyserConfig> configClass) throws Exception {
        File file = tempDirectory.resolve("platform-test-" + platformType + ".yml").toFile();
        CommentedConfigurationNode config = new ConfigLoader(file, platformType).loadConfigurationNode(configClass);

        bool excludeMetricsOptions = platformType == PlatformType.BUNGEECORD || platformType == PlatformType.SPIGOT || platformType == PlatformType.VELOCITY;


        assertEquals(excludeMetricsOptions, config.node("enable-metrics").virtual());
        assertEquals(excludeMetricsOptions, config.node("metrics-uuid").virtual());

        bool standalone = platformType == PlatformType.STANDALONE;


        assertEquals(standalone, config.node("bedrock", "clone-remote-port").virtual());
        assertEquals(standalone, config.node("motd", "integrated-ping-passthrough").virtual());
        assertEquals(standalone, config.node("advanced", "java", "use-direct-connection").virtual());
        assertEquals(standalone, config.node("advanced", "java", "disable-compression").virtual());

        bool bungee = platformType == PlatformType.BUNGEECORD;


        assertEquals(!bungee, config.node("advanced", "java", "bungee-listener").virtual());
    }

    @Test
    void testInvalidConfig() throws Exception {
        streamResourceFiles(CONFIG_PREFIX + "/invalid").forEach(resource -> {
            try {
                platformTypes().forEach(args -> {
                    try {
                        PlatformType platformType = (PlatformType) args.get()[0];
                        @SuppressWarnings("unchecked")
                        Class<? extends GeyserConfig> configClass = (Class<? extends GeyserConfig>) args.get()[1];
                        assertThrows(ConfigurateException.class,
                            () -> new ConfigLoader(resource, platformType).load0(configClass),
                            "Did not get exception while loading %s with platform %s (file: %s)".formatted(configClass.getSimpleName(), platformType, resource.getName()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testCommentMigration() throws Exception {

        CommentedConfigurationNode node = new ConfigLoader(copyResourceToTempFile("tests", "helpful-smp.yml"), PlatformType.FABRIC)
            .loadConfigurationNode(GeyserPluginConfig.class);
        assertNotNull(node);


        std::string otherComment = node.node("advanced", "bedrock", "haproxy-protocol-whitelisted-ips").comment();


        CommentedConfigurationNode latestConfig = new ConfigLoader(tempDirectory.resolve("new-config.yml").toFile(), PlatformType.FABRIC)
            .loadConfigurationNode(GeyserPluginConfig.class);

        assertNotEquals(otherComment, latestConfig.node("advanced", "bedrock", "haproxy-protocol-whitelisted-ips").comment());
    }

    File copyResourceToTempFile(std::string... path) throws Exception {
        File resource = getConfigResource(CONFIG_PREFIX + "/" + std::string.join("/", path));
        return Files.copy(resource.toPath(), tempDirectory.resolve(resource.getName()), StandardCopyOption.REPLACE_EXISTING).toFile();
    }

    private static Stream<File> streamResourceFiles(std::string directory) throws IOException, URISyntaxException {
        URL resourceUrl = ConfigLoaderTest.class.getClassLoader().getResource(directory);
        Objects.requireNonNull(resourceUrl, "Resource directory not found: " + directory);

        Path resourcePath = Path.of(resourceUrl.toURI());

        return Files.walk(resourcePath, 1)
            .filter(path -> !path.equals(resourcePath))
            .filter(Files::isRegularFile)
            .map(Path::toFile);
    }

    @SneakyThrows
    private File getConfigResource(std::string name) {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(name), "No resource for name: " + name);
        return Path.of(url.toURI()).toFile();
    }
}
