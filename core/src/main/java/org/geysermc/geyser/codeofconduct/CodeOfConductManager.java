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

package org.geysermc.geyser.codeofconduct;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CodeOfConductManager {
    private static final Path SAVE_PATH = Path.of("cache/codeofconducts.json");
    private static CodeOfConductManager loaded = null;

    private final ExecutorService saveService = Executors.newFixedThreadPool(1);
    private final Object2IntMap<String> playerAcceptedCodeOfConducts = new Object2IntOpenHashMap<>();

    private CodeOfConductManager() {}

    public boolean hasAcceptedCodeOfConduct(GeyserSession session, String codeOfConduct) {
        return playerAcceptedCodeOfConducts.getInt(session.xuid()) == codeOfConduct.hashCode();
    }

    public void saveCodeOfConduct(GeyserSession session, String codeOfConduct) {
        playerAcceptedCodeOfConducts.put(session.xuid(), codeOfConduct.hashCode());
        CompletableFuture.runAsync(this::save, saveService);
    }

    private void save() {
        JsonObject saved = new JsonObject();
        playerAcceptedCodeOfConducts.forEach(saved::addProperty);
        Path path = GeyserImpl.getInstance().configDirectory().resolve(SAVE_PATH);
        try {
            Files.writeString(path, saved.toString());
        } catch (IOException exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to write code of conduct cache!", exception);
        }
    }

    // TODO load at startup
    public static CodeOfConductManager getInstance() {
        if (loaded != null) {
            return loaded;
        }

        CodeOfConductManager manager = new CodeOfConductManager();
        Path path = GeyserImpl.getInstance().configDirectory().resolve(SAVE_PATH);
        if (Files.exists(path) && Files.isRegularFile(path)) {
            try (Reader reader = new FileReader(path.toFile())) {
                JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    manager.playerAcceptedCodeOfConducts.put(entry.getKey(), entry.getValue().getAsInt());
                }
            } catch (IOException exception) {
                GeyserImpl.getInstance().getLogger().error("Failed to read code of conduct cache!", exception);
            }
        }

        loaded = manager;
        return loaded;
    }
}
