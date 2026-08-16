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

package org.geysermc.geyser.pack.java.contents;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.util.MinecraftKey;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public record JavaPackContents(Map<Key, Map<String, String>> languages) {
    public static final JavaPackContents EMPTY = new JavaPackContents(Map.of());

    public static JavaPackContents read(FileSystem zip) throws IOException {
        return new JavaPackContents(readLanguages(zip));
    }

    private static Map<Key, Map<String, String>> readLanguages(FileSystem zip) throws IOException {
        return collectJson(zip, "lang", json -> json.getAsJsonObject().asMap().entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().intern(), entry -> entry.getValue().getAsString().intern())));
    }

    private static <T> Map<Key, T> collectJson(FileSystem zip, String type, IOFunction<JsonElement, T> parser) throws IOException {
        return collect(zip, type, "json", path -> parser.apply(new JsonParser().parse(Files.readString(path))));
    }

    private static <T> Map<Key, T> collect(FileSystem zip, String type, String extension, IOFunction<Path, T> reader) throws IOException {
        Map<Key, T> map = new Object2ObjectOpenHashMap<>();
        namespaceIterate(zip, type, extension, (key, path) -> map.put(key, reader.apply(path)));
        return Collections.unmodifiableMap(map);
    }

    private static void namespaceIterate(FileSystem zip, String type, String extension, IOBiConsumer<Key, Path> pathConsumer) throws IOException {
        Files.walkFileTree(assets(zip), new SimpleFileVisitor<>() {
            @Override
            public @NonNull FileVisitResult preVisitDirectory(@NonNull Path dir, @NonNull BasicFileAttributes attrs) {
                if (dir.getNameCount() < 3 || type.startsWith(dir.subpath(2, dir.getNameCount()).toString())) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                if (file.getNameCount() < 4 && !file.endsWith("." + extension)) {
                    return FileVisitResult.CONTINUE;
                }

                String namespace = file.getName(1).toString();
                String path = file.getFileName().toString().replaceFirst("\\." + extension + "$", "");

                Key key;
                try {
                    key = MinecraftKey.key(namespace, path);
                } catch (InvalidKeyException exception) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    pathConsumer.accept(key, file);
                } catch (Exception exception) {
                    if (exception instanceof IOException) {
                        throw exception;
                    }
                    // TODO note which pack
                    GeyserImpl.getInstance().getLogger().warning("Failed to read asset " + key + " in pack!");
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static Path assets(FileSystem zip) {
        return zip.getPath("assets");
    }

    @FunctionalInterface
    private interface IOBiConsumer<T1, T2> {

        void accept(T1 object1, T2 object) throws IOException;
    }

    @FunctionalInterface
    private interface IOFunction<T, R> {

        R apply(T object) throws IOException;
    }
}
