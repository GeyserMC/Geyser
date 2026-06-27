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

package org.geysermc.geyser.session;

import org.geysermc.geyser.GeyserLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Loads the {@link EducationUuidScheme} from {@code <config>/uuid/scheme.yml}.
 * <p>
 * Because this controls player identity, the loader is deliberately strict: an absent file
 * means "no operator intent", so it is created from the template and defaults to MODERN; but
 * a file that is present yet unparseable or set to an unrecognized value is treated as a fatal
 * misconfiguration that aborts startup, rather than silently guessing a scheme and re-IDing
 * every education player.
 */
public final class EducationUuidSchemeLoader {

    private static final String FOLDER_NAME = "uuid";
    private static final String FILE_NAME = "scheme.yml";

    private static final String TEMPLATE =
            "# Education player UUID scheme.\n" +
            "#\n" +
            "# DO NOT change unless you know exactly what you're doing or were instructed to.\n" +
            "# This controls how education players' UUIDs (their identity for permissions,\n" +
            "# data, balances) are derived. It MUST be identical on EduGeyser and EVERY\n" +
            "# EduFloodgate instance on the network, including all backend servers and\n" +
            "# proxies. A mismatch gives a player different UUIDs on different servers and\n" +
            "# corrupts their data. Changing it on a live server re-IDs every education player.\n" +
            "#\n" +
            "#   modern - (default) MESS-verified Entra OID. Recommended for all new servers.\n" +
            "#   legacy - SHA-256(tenantId:username). Only for existing legacy player data.\n" +
            "scheme: modern\n";

    private EducationUuidSchemeLoader() {
    }

    public static EducationUuidScheme load(Path configFolder, GeyserLogger logger) {
        Path folder = configFolder.resolve(FOLDER_NAME);
        Path file = folder.resolve(FILE_NAME);

        // Absent file: no operator intent yet. Write the template and default to modern.
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(folder);
                Files.writeString(file, TEMPLATE);
            } catch (IOException e) {
                logger.warning("[Education UUID] Could not write " + file + ": " + e.getMessage());
            }
            return EducationUuidScheme.MODERN;
        }

        // Present file: operator intent. Never guess; any problem is a hard fail.
        String raw;
        try {
            var loader = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                    .path(file).build();
            raw = loader.load().node("scheme").getString();
        } catch (Exception e) {
            throw refuseToStart(logger, file, "the file could not be parsed (" + e.getMessage() + ")");
        }

        if (raw == null || raw.isBlank()) {
            throw refuseToStart(logger, file, "no 'scheme' value is set");
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "modern" -> EducationUuidScheme.MODERN;
            case "legacy" -> EducationUuidScheme.LEGACY;
            default -> throw refuseToStart(logger, file,
                    "'" + raw.trim() + "' is not a valid scheme (use modern or legacy)");
        };
    }

    private static RuntimeException refuseToStart(GeyserLogger logger, Path file, String reason) {
        logger.error("************************************************************************");
        logger.error("  EDUCATION UUID SCHEME IS MISCONFIGURED - REFUSING TO START");
        logger.error("");
        logger.error("  File:    " + file);
        logger.error("  Problem: " + reason + ".");
        logger.error("");
        logger.error("  This setting controls how education players' UUIDs (their identity)");
        logger.error("  are derived. Guessing it could silently change every education");
        logger.error("  player's UUID and disconnect them from their data, so EduGeyser will");
        logger.error("  not start until it is fixed.");
        logger.error("");
        logger.error("  Set 'scheme' to exactly 'modern' or 'legacy', or delete the file to");
        logger.error("  regenerate it as 'modern'.");
        logger.error("************************************************************************");
        return new IllegalStateException("Invalid education UUID scheme in " + file + ": " + reason);
    }
}
