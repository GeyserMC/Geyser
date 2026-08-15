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

package org.geysermc.geyser.biome.custom;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.biome.custom.CustomBiomeAppearance;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@EqualsAndHashCode
@ToString
public final class GeyserCustomBiomeDefinition implements CustomBiomeDefinition {
    // Stricter than Java's identifier rules, which also allow '/'
    private static final Pattern BEDROCK_IDENTIFIER = Pattern.compile("^[a-z0-9._-]+:[a-z0-9._-]+$");
    private static final Pattern BIOME_TAG = Pattern.compile("^[a-z0-9_.]+(:[a-z0-9_.]+)?$");

    private final Identifier bedrockIdentifier;
    private final Set<String> tags;
    private final @Nullable CustomBiomeAppearance appearance;

    public GeyserCustomBiomeDefinition(Builder builder) {
        String identifier = builder.bedrockIdentifier.toString();
        if (!BEDROCK_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                "Bedrock biome identifiers must match " + BEDROCK_IDENTIFIER.pattern() + ", got " + identifier);
        }
        if (Identifier.DEFAULT_NAMESPACE.equals(builder.bedrockIdentifier.namespace())) {
            throw new IllegalArgumentException(
                "Custom biomes cannot use the minecraft namespace: " + identifier);
        }
        if (!builder.derived && "geyser".equals(builder.bedrockIdentifier.namespace()) && builder.bedrockIdentifier.path().startsWith("auto_")) {
            throw new IllegalArgumentException(
                "The geyser:auto_ prefix is reserved for Geyser: " + identifier);
        }
        if (builder.appearance != null && !(builder.appearance instanceof GeyserCustomBiomeAppearance)) {
            throw new IllegalArgumentException(
                "The appearance for " + identifier + " was not created with CustomBiomeAppearance.builder()");
        }
        for (String tag : builder.tags) {
            if (!BIOME_TAG.matcher(tag).matches()) {
                throw new IllegalArgumentException(
                    "Biome tags must match " + BIOME_TAG.pattern() + ", got " + tag);
            }
            if (tag.startsWith("minecraft:")) {
                throw new IllegalArgumentException(
                    "Biome tags cannot use the minecraft: prefix: " + tag);
            }
        }

        this.bedrockIdentifier = builder.bedrockIdentifier;
        // Sorted so tag order doesn't change how a definition serializes
        this.tags = Collections.unmodifiableSortedSet(new TreeSet<>(builder.tags));
        this.appearance = builder.appearance;
    }

    /**
     * Creates the builder for a Java biome mapping that doesn't name a Bedrock identifier,
     * deriving one from the Java identifier instead.
     */
    public static Builder derivedBuilder(Identifier javaIdentifier) {
        Builder builder = new Builder(deriveBedrockIdentifier(javaIdentifier));
        builder.derived = true;
        return builder;
    }

    /**
     * Java identifiers that are valid custom Bedrock identifiers are used as they are; for
     * others, a digest of the full identifier is used, since flattening characters like
     * {@code /} could make different Java identifiers collide.
     */
    private static Identifier deriveBedrockIdentifier(Identifier javaIdentifier) {
        String identifier = javaIdentifier.toString();
        if (BEDROCK_IDENTIFIER.matcher(identifier).matches()
                && !Identifier.DEFAULT_NAMESPACE.equals(javaIdentifier.namespace())
                && !("geyser".equals(javaIdentifier.namespace()) && javaIdentifier.path().startsWith("auto_"))) {
            return javaIdentifier;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(identifier.getBytes(StandardCharsets.UTF_8));
            return Identifier.of("geyser", "auto_" + HexFormat.of().formatHex(digest, 0, 16));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public Identifier bedrockIdentifier() {
        return bedrockIdentifier;
    }

    @Override
    public Set<String> tags() {
        return tags;
    }

    @Override
    public @Nullable CustomBiomeAppearance appearance() {
        return appearance;
    }

    public static class Builder implements CustomBiomeDefinition.Builder {
        private final Identifier bedrockIdentifier;
        private final Set<String> tags = new HashSet<>();
        private @Nullable CustomBiomeAppearance appearance;
        private boolean derived;

        public Builder(Identifier bedrockIdentifier) {
            this.bedrockIdentifier = Objects.requireNonNull(bedrockIdentifier, "bedrockIdentifier may not be null");
        }

        @Override
        public Builder tag(String tag) {
            this.tags.add(Objects.requireNonNull(tag, "tag may not be null"));
            return this;
        }

        @Override
        public Builder appearance(CustomBiomeAppearance appearance) {
            this.appearance = Objects.requireNonNull(appearance, "appearance may not be null");
            return this;
        }

        @Override
        public CustomBiomeDefinition build() {
            return new GeyserCustomBiomeDefinition(this);
        }
    }
}
