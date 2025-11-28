/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

import org.geysermc.geyser.GeyserBootstrap;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.TransformAction;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.spongepowered.configurate.NodePath.path;
import static org.spongepowered.configurate.transformation.TransformAction.remove;
import static org.spongepowered.configurate.transformation.TransformAction.rename;

public class ConfigMigrations {

    public static final BiFunction<Class<? extends GeyserConfig>, GeyserBootstrap, ConfigurationTransformation.Versioned> TRANSFORMER = (configClass, bootstrap) ->
        ConfigurationTransformation.versionedBuilder()
        .versionKey("config-version")
        .addVersion(5, ConfigurationTransformation.builder()
            // Java section
            .addAction(path("remote"), rename("java"))
            .addAction(path("remote", "address"), (path, value) -> {
                if ("auto".equals(value.getString())) {
                    // Auto-convert back to localhost
                    value.set("127.0.0.1");
                }
                return null;
            })

            // Motd section
            .addAction(path("bedrock", "motd1"), renameAndMove("motd", "primary-motd"))
            .addAction(path("bedrock", "motd2"), renameAndMove("motd", "secondary-motd"))
            .addAction(path("passthrough-motd"), moveTo("motd"))
            .addAction(path("passthrough-player-counts"), moveTo("motd"))
            .addAction(path("ping-passthrough-interval"), moveTo("motd"))
            .addAction(path("max-players"), moveTo("motd"))
            .addAction(path("legacy-ping-passthrough"), configClass == GeyserRemoteConfig.class ? remove() : (path, value) -> {
                // Invert value
                value.set(!value.getBoolean());
                return new Object[]{ "motd", "integrated-ping-passthrough" };
            })

            // gameplay
            .addAction(path("command-suggestions"), moveTo("gameplay"))
            .addAction(path("forward-player-ping"), moveTo("gameplay"))
            .addAction(path("show-cooldown"), (path, value) -> {
                String s = value.getString();
                if (s != null) {
                    switch (s) {
                        case "true" -> value.set("title");
                        case "false" -> value.set("disabled");
                    }
                }
                return new Object[]{ "gameplay", "show-cooldown" };
            })
            .addAction(path("bedrock", "server-name"), moveTo("gameplay"))
            .addAction(path("show-coordinates"), moveTo("gameplay"))
            .addAction(path("disable-bedrock-scaffolding"), moveTo("gameplay"))
            .addAction(path("custom-skull-render-distance"), moveTo("gameplay"))
            .addAction(path("force-resource-packs"), moveTo("gameplay"))
            .addAction(path("xbox-achievements-enabled"), moveTo("gameplay"))
            .addAction(path("unusable-space-block"), moveTo("gameplay"))
            .addAction(path("unusable-space-block"), moveTo("gameplay"))
            .addAction(path("add-non-bedrock-items"), renameAndMove("gameplay", "enable-custom-content"))
            .addAction(path("above-bedrock-nether-building"), renameAndMove("gameplay", "nether-roof-workaround"))
            .addAction(path("xbox-achievements-enabled"), moveTo("gameplay"))
            // NOTE: We're not explicitly removing the allow-custom-skulls option, it will already be removed since it
            // won't be written back. If we remove it, we can't query the value of it!
            .addAction(path("max-visible-custom-skulls"), (path, value) -> {
                ConfigurationNode parent = value.parent();
                if (parent != null && parent.isMap()) {
                    ConfigurationNode allowCustomSkulls = parent.childrenMap().get("allow-custom-skulls");
                    if (allowCustomSkulls != null && !allowCustomSkulls.getBoolean()) {
                        value.set(0);
                    }
                }
                return new Object[]{ "gameplay", "max-visible-custom-skulls" };
            })
            .addAction(path("emote-offhand-workaround"), (path,  value) -> {
                String previous = value.getString();
                if (!Objects.equals(previous, "disabled") && bootstrap != null) {
                    bootstrap.getGeyserLogger().warning("The emote-offhand-workaround option has been removed from Geyser. If you still wish to have this functionality, use this Geyser extension: https://github.com/GeyserMC/EmoteOffhandExtension/");
                }
                if (Objects.equals(previous, "no-emotes")) {
                    value.set(false);
                    return new Object[]{ "gameplay", "show-emotes" };
                }
                return null;
            })

            // For the warning!
            .addAction(path("allow-third-party-capes"), (node, value) -> {
                if (bootstrap != null) {
                    bootstrap.getGeyserLogger().warning("Third-party ears/capes have been removed from Geyser. If you still wish to have this functionality, use this Geyser extension: https://github.com/GeyserMC/ThirdPartyCosmetics");
                }
                return null;
            })
            .addAction(path("allow-third-party-ears"), (node, value) -> {
                if (bootstrap != null) {
                    bootstrap.getGeyserLogger().warning("Third-party ears/capes have been removed from Geyser. If you still wish to have this functionality, use this Geyser extension: https://github.com/GeyserMC/ThirdPartyCosmetics");
                }
                return null;
            })

            // Advanced section
            .addAction(path("cache-images"), moveTo("advanced"))
            .addAction(path("scoreboard-packet-threshold"), moveTo("advanced"))
            .addAction(path("add-team-suggestions"), moveTo("advanced"))
            .addAction(path("floodgate-key-file"), (path, value) -> {
                // Elimate any legacy config values
                if ("public-key.pem".equals(value.getString())) {
                    value.set("key.pem");
                }
                return new Object[]{ "advanced", "floodgate-key-file" };
            })

            // Bedrock
            .addAction(path("bedrock", "broadcast-port"), moveTo("advanced", "bedrock"))
            .addAction(path("bedrock", "compression-level"), moveTo("advanced", "bedrock"))
            .addAction(path("bedrock", "enable-proxy-protocol"), renameAndMove("advanced", "bedrock", "use-haproxy-protocol"))
            .addAction(path("bedrock", "proxy-protocol-whitelisted-ips"), renameAndMove("advanced", "bedrock", "haproxy-protocol-whitelisted-ips"))
            .addAction(path("mtu"), moveTo("advanced", "bedrock"))

            // Java
            .addAction(path("remote", "use-proxy-protocol"), renameAndMove("advanced", "java", "use-haproxy-protocol"))
            .addAction(path("disable-compression"), moveTo("advanced", "java"))
            .addAction(path("use-direct-connection"), moveTo("advanced", "java"))

            // Other
            .addAction(path("default-locale"), (path, value) -> {
                if (value.getString() == null) {
                    value.set("system");
                }
                return null;
            })
            .addAction(path("metrics", "uuid"), (path, value) -> {
                if ("generateduuid".equals(value.getString())) {
                    // Manually copied config without Metrics UUID creation?
                    value.set(UUID.randomUUID());
                }
                return new Object[]{ "metrics-uuid" };
            })
            .addAction(path("metrics", "enabled"), (path, value) -> {
                // Move to the root, not in the Metrics class.
                return new Object[]{ "enable-metrics" };
            })

            .build())
        .build();

    static TransformAction renameAndMove(String... newPath) {
        return ((path, value) -> Arrays.stream(newPath).toArray());
    }

    static TransformAction moveTo(String... newPath) {
        return (path, value) -> {
            Object[] arr = path.array();
            if (arr.length == 0) {
                throw new ConfigurateException(value, "The root node cannot be renamed!");
            } else {
                // create a new array with space for newPath segments + the original last segment
                Object[] result = new Object[newPath.length + 1];
                System.arraycopy(newPath, 0, result, 0, newPath.length);
                result[newPath.length] = arr[arr.length - 1];
                return result;
            }
        };
    }
}
