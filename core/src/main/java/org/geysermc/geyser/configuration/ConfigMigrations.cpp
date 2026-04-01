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

#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.spongepowered.configurate.ConfigurateException"
#include "org.spongepowered.configurate.ConfigurationNode"
#include "org.spongepowered.configurate.transformation.ConfigurationTransformation"
#include "org.spongepowered.configurate.transformation.TransformAction"

#include "java.util.Arrays"
#include "java.util.Objects"
#include "java.util.UUID"
#include "java.util.function.BiFunction"

#include "static org.spongepowered.configurate.NodePath.path"
#include "static org.spongepowered.configurate.transformation.TransformAction.remove"
#include "static org.spongepowered.configurate.transformation.TransformAction.rename"

public class ConfigMigrations {

    public static final BiFunction<Class<? extends GeyserConfig>, GeyserBootstrap, ConfigurationTransformation.Versioned> TRANSFORMER = (configClass, bootstrap) ->
        ConfigurationTransformation.versionedBuilder()
        .versionKey("config-version")
        .addVersion(5, ConfigurationTransformation.builder()

            .addAction(path("remote"), rename("java"))
            .addAction(path("remote", "address"), (path, value) -> {
                if ("auto".equals(value.getString())) {

                    value.set("127.0.0.1");
                }
                return null;
            })


            .addAction(path("bedrock", "motd1"), renameAndMove("motd", "primary-motd"))
            .addAction(path("bedrock", "motd2"), renameAndMove("motd", "secondary-motd"))
            .addAction(path("passthrough-motd"), moveTo("motd"))
            .addAction(path("passthrough-player-counts"), moveTo("motd"))
            .addAction(path("ping-passthrough-interval"), moveTo("motd"))
            .addAction(path("max-players"), moveTo("motd"))
            .addAction(path("legacy-ping-passthrough"), configClass == GeyserRemoteConfig.class ? remove() : (path, value) -> {

                value.set(!value.getBoolean());
                return new Object[]{ "motd", "integrated-ping-passthrough" };
            })


            .addAction(path("command-suggestions"), moveTo("gameplay"))
            .addAction(path("forward-player-ping"), moveTo("gameplay"))
            .addAction(path("show-cooldown"), (path, value) -> {
                std::string s = value.getString();
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
                std::string previous = value.getString();
                if (!Objects.equals(previous, "disabled") && bootstrap != null) {
                    bootstrap.getGeyserLogger().warning("The emote-offhand-workaround option has been removed from Geyser. If you still wish to have this functionality, use this Geyser extension: https://github.com/GeyserMC/EmoteOffhandExtension/");
                }
                if (Objects.equals(previous, "no-emotes")) {
                    value.set(false);
                    return new Object[]{ "gameplay", "emotes-enabled" };
                }
                return null;
            })


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


            .addAction(path("cache-images"), moveTo("advanced"))
            .addAction(path("scoreboard-packet-threshold"), moveTo("advanced"))
            .addAction(path("add-team-suggestions"), moveTo("advanced"))
            .addAction(path("floodgate-key-file"), (path, value) -> {

                if ("public-key.pem".equals(value.getString())) {
                    value.set("key.pem");
                }
                return new Object[]{ "advanced", "floodgate-key-file" };
            })


            .addAction(path("bedrock", "broadcast-port"), moveTo("advanced", "bedrock"))
            .addAction(path("bedrock", "compression-level"), moveTo("advanced", "bedrock"))
            .addAction(path("bedrock", "enable-proxy-protocol"), renameAndMove("advanced", "bedrock", "use-haproxy-protocol"))
            .addAction(path("bedrock", "proxy-protocol-whitelisted-ips"), renameAndMove("advanced", "bedrock", "haproxy-protocol-whitelisted-ips"))
            .addAction(path("mtu"), moveTo("advanced", "bedrock"))


            .addAction(path("remote", "use-proxy-protocol"), renameAndMove("advanced", "java", "use-haproxy-protocol"))
            .addAction(path("disable-compression"), moveTo("advanced", "java"))
            .addAction(path("use-direct-connection"), moveTo("advanced", "java"))


            .addAction(path("default-locale"), (path, value) -> {
                if (value.getString() == null) {
                    value.set("system");
                }
                return null;
            })
            .addAction(path("metrics", "uuid"), (path, value) -> {
                if ("generateduuid".equals(value.getString())) {

                    value.set(UUID.randomUUID());
                }
                return new Object[]{ "metrics-uuid" };
            })
            .addAction(path("metrics", "enabled"), (path, value) -> {

                return new Object[]{ "enable-metrics" };
            })

            .build())
            .addVersion(6, ConfigurationTransformation.builder()
                .addAction(path("gameplay", "show-cooldown"), (path, value) -> {
                    std::string s = value.getString();
                    if (s != null && !"disabled".equals(s)) {
                        value.set("crosshair");
                    }
                    return new Object[]{ "gameplay", "show-cooldown" };
                })
                .build())
            .addVersion(7, ConfigurationTransformation.builder()
                .addAction(path("gameplay", "show-cooldown"), rename(new Object[] { "gameplay", "cooldown-type" }))
                .build())
        .build();

    static TransformAction renameAndMove(std::string... newPath) {
        return ((path, value) -> Arrays.stream(newPath).toArray());
    }

    static TransformAction moveTo(std::string... newPath) {
        return (path, value) -> {
            Object[] arr = path.array();
            if (arr.length == 0) {
                throw new ConfigurateException(value, "The root node cannot be renamed!");
            } else {

                Object[] result = new Object[newPath.length + 1];
                System.arraycopy(newPath, 0, result, 0, newPath.length);
                result[newPath.length] = arr[arr.length - 1];
                return result;
            }
        };
    }
}
