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

package org.geysermc.geyser.network.bedrock.nethernet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Files written by the nethernet record and packets commands, and when a connection drops.
 */
public final class NetherNetRecordFiles {
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT);

    private NetherNetRecordFiles() {
    }

    /**
     * @return a new file in the nethernet-records folder next to the config, named after the player and the time
     */
    public static Path path(Path configDirectory, String name, String extension) {
        return configDirectory.resolve("nethernet-records")
            .resolve(name.replaceAll("[^A-Za-z0-9_-]", "_") + "-" + FILE_STAMP.format(LocalDateTime.now()) + extension);
    }

    /**
     * Creates the file with its comment lines and column header.
     *
     * @param comments lines written first, each prefixed with "# "
     */
    static BufferedWriter open(Path file, List<String> comments, String columns) throws IOException {
        Files.createDirectories(file.getParent());
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            for (String comment : comments) {
                writer.write("# " + comment);
                writer.newLine();
            }
            writer.write(columns);
            writer.newLine();
            return writer;
        } catch (IOException e) {
            writer.close();
            throw e;
        }
    }
}
