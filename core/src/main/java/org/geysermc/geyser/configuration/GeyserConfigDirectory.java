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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record GeyserConfigDirectory(Path root) {
    public static final Path CACHE = Path.of("cache");
    public static final Path CACHE_DEFAULT_PLAYER_SKINS_SLIM = Path.of("cache/default_player_skins/slim");
    public static final Path CACHE_DEFAULT_PLAYER_SKINS_WIDE = Path.of("cache/default_player_skins/wide");
    public static final Path CACHE_IMAGES = Path.of("cache/images");
    public static final Path CACHE_PLAYER_SKULLS = Path.of("cache/player_skulls");
    public static final Path CACHE_REMOTE_PACKS = Path.of("cache/remote_packs");
    public static final Path CUSTOM_MAPPINGS = Path.of("custom_mappings");
    public static final Path EXTENSIONS = Path.of("extensions");
    public static final Path LANGUAGES = Path.of("languages");
    public static final Path LOCALES = Path.of("locales");
    public static final Path LOCALES_OVERRIDES = Path.of("locales/overrides");
    public static final Path NETHERNET = Path.of("nethernet");
    public static final Path PACKS = Path.of("packs");

    public Path resolve(Path other) {
        return root.resolve(other);
    }

    public Path createIfAbsent(Path directory) {
        Path resolved = resolve(directory);
        if (Files.exists(resolved)) {
            if (!Files.isDirectory(resolved)) {
                throw new IllegalStateException("Expected " + directory + " in config to be a directory, was a file");
            }
            return resolved;
        }
        try {
            Files.createDirectories(resolved);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to create directory in config: " + directory, exception);
        }
        return resolved;
    }
}
