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

package org.geysermc.geyser.registry.mappings;

import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class MappingsConfigReaderTest {

    @TempDir
    Path configFolder;

    @ParameterizedTest
    @ValueSource(strings = {
        "{ \"format_version\": 1, \"skulls\": {",
        "[]",
        "",
        "{ \"format_version\": \"one\", \"skulls\": {} }",
        "{ \"format_version\": {}, \"skulls\": {} }"
    })
    void invalidFileIsSkipped(String contents) throws IOException {
        Path file = Files.writeString(configFolder.resolve("invalid.json"), contents);
        Map<GeyserDefineCustomSkullsEvent.SkullTextureType, List<String>> map = new EnumMap<>(GeyserDefineCustomSkullsEvent.SkullTextureType.class);

        GeyserMockContext.mockContext(() -> MappingsConfigReader.readCustomMappings(MappingsType.SKULLS, file, map::put));

        assertTrue(map.isEmpty());
    }

    @Test
    void invalidFileDoesNotStopOtherFiles() throws IOException, URISyntaxException {
        Path customMappings = Files.createDirectory(configFolder.resolve("custom_mappings"));
        Files.writeString(customMappings.resolve("invalid.json"), "{ \"format_version\": 1, \"skulls\": {");
        Path validFile = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("configuration/custom-skulls.json")).toURI());
        Files.copy(validFile, customMappings.resolve("valid.json"));
        Map<GeyserDefineCustomSkullsEvent.SkullTextureType, List<String>> map = new EnumMap<>(GeyserDefineCustomSkullsEvent.SkullTextureType.class);

        GeyserMockContext.mockContext(context -> {
            GeyserBootstrap bootstrap = context.mock(GeyserBootstrap.class);
            when(bootstrap.getConfigFolder()).thenReturn(configFolder);
            when(context.mockOrSpy(GeyserImpl.class).getBootstrap()).thenReturn(bootstrap);

            MappingsConfigReader.loadCustomMappingsFromJson(MappingsType.SKULLS, map::put);
        });

        assertEquals("GeyserMC", map.get(GeyserDefineCustomSkullsEvent.SkullTextureType.USERNAME).getFirst());
    }
}
