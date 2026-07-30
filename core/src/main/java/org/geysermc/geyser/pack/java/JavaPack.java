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

package org.geysermc.geyser.pack.java;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

// com.google.common.hash
@SuppressWarnings("UnstableApiUsage")
public record JavaPack(Id id, Contents contents) {
    private static final HashFunction HASHER = Hashing.sha1();

    static Optional<JavaPack> open(Path path, Id id) {
        // TODO Logs
        try {
            if (id.hash != null) {
                System.out.println("trying to hash downloaded pack");
                HashCode fileHash = HASHER.hashBytes(Files.readAllBytes(path));
                System.out.println("computed hash");
                if (!fileHash.equals(id.hash)) {
                    GeyserImpl.getInstance().getLogger().error("Failed to load server resourcepack with UUID " + id.uuid + " because the hash did not match " +
                        "(ours: \"" + fileHash + "\", theirs: \"" + id.hash + "\")");
                    return Optional.empty();
                }
            }

            try (FileSystem zip = FileSystems.newFileSystem(path)) {
                System.out.println("opening and returning");
                return Optional.of(new JavaPack(id, Contents.read(zip)));
            }
        } catch (IOException exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to load server resourcepack with UUID " + id.uuid + "!", exception);
            return Optional.empty();
        }
    }

    public record Id(UUID uuid, @Nullable HashCode hash) {}

    public record Contents(Map<Key, Map<String, String>> languages) {
        public static final Contents EMPTY = new Contents(Map.of());

        private static Contents read(FileSystem zip) {
            // FIXME
            return EMPTY;
        }
    }

    public record Composed(Contents contents, Map<String, String> flattenedTranslations) {
        public static final Composed EMPTY = new Composed(Contents.EMPTY, Map.of());

        public static Composed composePacks(Stream<JavaPack.Contents> packs) {
            // FIXME
            return EMPTY;
        }
    }
}
