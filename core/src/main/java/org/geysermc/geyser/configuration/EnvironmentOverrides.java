/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserLogger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Overrides config values with environment variables, e.g. {@code bedrock.port} -> {@code GEYSER_BEDROCK_PORT}.
 * List values are comma-separated. Overrides are never written to the config file.
 */
final class EnvironmentOverrides {
    private static final String PREFIX = "GEYSER";
    private static final String CONFIG_VERSION_KEY = "GEYSER_CONFIG_VERSION";

    private EnvironmentOverrides() {
    }

    /**
     * @param platformExcluded paths removed from the config because they do not apply to this platform
     * @return whether any value was overridden
     */
    static boolean apply(CommentedConfigurationNode root, Set<NodePath> platformExcluded,
                         Map<String, String> env, @Nullable GeyserLogger logger) throws SerializationException {
        Map<String, String> remaining = new Object2ObjectOpenHashMap<>();
        env.forEach((key, value) -> {
            if (key.startsWith(PREFIX + "_")) {
                remaining.put(key, value);
            }
        });
        if (remaining.isEmpty()) {
            return false;
        }

        // The config version is only read by migrations, which have already run at this point
        if (remaining.remove(CONFIG_VERSION_KEY) != null && logger != null) {
            logger.warning("The config version cannot be set via environment variables; ignoring " + CONFIG_VERSION_KEY);
        }

        boolean changed = visit(root, remaining, logger);

        // Options that exist, just not on this platform
        for (NodePath path : platformExcluded) {
            String excludedKey = envKey(path);
            remaining.keySet().removeIf(key -> key.equals(excludedKey) || key.startsWith(excludedKey + "_"));
        }

        if (logger != null) {
            for (String key : remaining.keySet()) {
                logger.warning("Environment variable " + key + " does not match any config option");
            }
        }
        return changed;
    }

    private static boolean visit(CommentedConfigurationNode node, Map<String, String> remaining,
                                 @Nullable GeyserLogger logger) throws SerializationException {
        if (node.isMap()) {
            boolean changed = false;
            for (CommentedConfigurationNode child : node.childrenMap().values()) {
                changed |= visit(child, remaining, logger);
            }
            return changed;
        }

        String key = envKey(node.path());
        String value = remaining.remove(key);
        if (value == null) {
            return false;
        }

        if (node.isList()) {
            node.setList(String.class, splitList(value));
        } else {
            // Configurate's scalar serializers coerce strings, so type conversion happens on deserialization
            node.raw(value);
        }

        if (logger != null) {
            logger.info("Config option " + node.path() + " set from environment variable " + key);
        }
        return true;
    }

    private static List<String> splitList(String value) {
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isEmpty())
            .toList();
    }

    static String envKey(NodePath path) {
        StringJoiner joiner = new StringJoiner("_");
        joiner.add(PREFIX);
        for (Object part : path) {
            joiner.add(part.toString().replace('-', '_').toUpperCase(Locale.ROOT));
        }
        return joiner.toString();
    }
}
