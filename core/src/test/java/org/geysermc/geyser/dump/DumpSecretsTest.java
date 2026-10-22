package org.geysermc.geyser.dump;

import com.google.gson.Gson;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.network.bedrock.nethernet.NetherNetServer.Advertised;
import org.geysermc.geyser.text.AsteriskSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A dump is posted to a public paste by default, so anything secret in the config has to be censored
 * on the way out, including in a full dump.
 */
class DumpSecretsTest {
    private static final String CONFIG = """
        mode: nxs
        advertise-addresses: ["198.51.100.1", "192.168.1.5", "::", "198.51.100.2:19133", "test.geysermc.org"]
        nxs:
          token: yaml-secret
        builtin:
          https:
            certificate: cert.pem
            private-key: key.pem
            password: keystore-secret
        """;

    @AfterEach
    void resetSensitivity() {
        AsteriskSerializer.showSensitive = false;
    }

    /** The same node the dump is built from, so the test cannot drift away from it. */
    private static CommentedConfigurationNode dumped() throws Exception {
        GeyserConfig.SignalingConfig config = YamlConfigurationLoader.builder()
            .source(() -> new BufferedReader(new StringReader(CONFIG)))
            .defaultOptions(InterfaceDefaultOptions::addTo)
            .build().load().get(GeyserConfig.SignalingConfig.class);

        CommentedConfigurationNode node = CommentedConfigurationNode.root(AsteriskSerializer.dumpOptions());
        node.set(config);
        return node;
    }

    @Test
    void censorsTheTokenAndKeystorePassword() throws Exception {
        CommentedConfigurationNode node = dumped();

        assertEquals("***", node.node("nxs", "token").getString());
        assertEquals("***", node.node("builtin", "https", "password").getString());
        assertFalse(node.toString().contains("yaml-secret"));
        assertFalse(node.toString().contains("keystore-secret"));
    }

    @Test
    void censorsThemInAFullDumpToo() throws Exception {
        // "dump full" only ever unmasks, which is why a secret cannot rely on the asterisk processor
        AsteriskSerializer.showSensitive = true;
        CommentedConfigurationNode node = dumped();

        assertEquals("***", node.node("nxs", "token").getString());
        assertEquals("***", node.node("builtin", "https", "password").getString());
        assertFalse(node.toString().contains("yaml-secret"));
        assertFalse(node.toString().contains("keystore-secret"));
    }

    @Test
    void leavesCertificatePathsAndUnsetSecretsReadable() throws Exception {
        CommentedConfigurationNode node = dumped();

        // Paths are not secret and are worth having when reading a dump for support
        assertEquals("cert.pem", node.node("builtin", "https", "certificate").getString());
        assertEquals("key.pem", node.node("builtin", "https", "private-key").getString());
    }

    @Test
    void censorsPublicAdvertisedAddresses() throws Exception {
        CommentedConfigurationNode node = dumped();

        // Local and wildcard entries stay readable, as with the bind address
        assertEquals(List.of("***", "192.168.1.5", "::", "***", "***"), node.node("advertise-addresses").getList(String.class));
    }

    @Test
    void showsAdvertisedAddressesInAFullDump() throws Exception {
        AsteriskSerializer.showSensitive = true;
        CommentedConfigurationNode node = dumped();

        assertEquals(List.of("198.51.100.1", "192.168.1.5", "::", "198.51.100.2:19133", "test.geysermc.org"),
            node.node("advertise-addresses").getList(String.class));
    }

    /** A public address found with STUN, and NXS confirming a public IPv4, a public IPv6 and a LAN address. */
    private static final Advertised ADVERTISED = new Advertised(
        new Advertised.Builtin(List.of(), true, List.of(new InetSocketAddress("198.51.100.1", 40404))),
        new Advertised.Nxs(new ProviderClient.PublishedEndpoints(List.of(new InetSocketAddress("198.51.100.1", 19132),
            new InetSocketAddress("2001:db8::1", 19133), new InetSocketAddress("192.168.1.5", 19132)), Set.of(6, 4))));

    @Test
    void censorsPublicAddressesNetherNetAdvertisesButKeepsTheirPorts() {
        assertEquals("{\"builtin\":{\"advertised\":[],\"stunDiscovery\":true,\"discovered\":[\"***:40404\"]},"
                + "\"nxs\":{\"confirmed\":true,\"addresses\":[\"***:19132\",\"[***]:19133\",\"192.168.1.5:19132\"],"
                + "\"assistedFamilies\":[4,6]}}",
            new Gson().toJson(DumpInfo.NetherNetInfo.of(ADVERTISED)));
    }

    @Test
    void showsAddressesNetherNetAdvertisesInAFullDump() {
        AsteriskSerializer.showSensitive = true;
        DumpInfo.NetherNetInfo info = DumpInfo.NetherNetInfo.of(ADVERTISED);

        assertEquals(List.of("198.51.100.1:40404"), info.builtin().discovered());
        assertEquals(List.of("198.51.100.1:19132", "[2001:db8::1]:19133", "192.168.1.5:19132"), info.nxs().addresses());
    }

    @Test
    void tellsNxsRunningWithoutConfirmedAddressesFromNotRunning() {
        assertEquals("{\"nxs\":{\"confirmed\":false,\"addresses\":[],\"assistedFamilies\":[]}}",
            new Gson().toJson(DumpInfo.NetherNetInfo.of(new Advertised(null, new Advertised.Nxs(null)))));
        assertEquals("{}", new Gson().toJson(DumpInfo.NetherNetInfo.of(new Advertised(null, null))));
    }
}
