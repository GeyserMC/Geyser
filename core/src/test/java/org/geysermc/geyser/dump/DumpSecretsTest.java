package org.geysermc.geyser.dump;

import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.text.AsteriskSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A dump is posted to a public paste by default, so anything secret in the config has to be censored
 * on the way out, including in a full dump.
 */
class DumpSecretsTest {
    private static final String CONFIG = """
        mode: nxs
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
        GeyserConfig.SignallingConfig config = YamlConfigurationLoader.builder()
            .source(() -> new BufferedReader(new StringReader(CONFIG)))
            .defaultOptions(InterfaceDefaultOptions::addTo)
            .build().load().get(GeyserConfig.SignallingConfig.class);

        ConfigurationOptions options = InterfaceDefaultOptions.addTo(ConfigurationOptions.defaults(), builder ->
                builder.addProcessor(AsteriskSerializer.Asterisk.class, String.class, AsteriskSerializer.CONFIGURATE_SERIALIZER)
                    .addProcessor(AsteriskSerializer.Secret.class, String.class, AsteriskSerializer.CONFIGURATE_SECRET))
            .shouldCopyDefaults(false);

        CommentedConfigurationNode node = CommentedConfigurationNode.root(options);
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
}
