/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.registry.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.world.particle.ParticleType;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.connector.registry.type.ParticleMapping;

import java.util.Iterator;
import java.util.Map;

public class ParticleTypesRegistryLoader extends EffectRegistryLoader<Map<ParticleType, ParticleMapping>> {

    @Override
    public Map<ParticleType, ParticleMapping> load(String input) {
        this.loadFile(input);

        Iterator<Map.Entry<String, JsonNode>> particlesIterator = this.get(input).fields();
        Map<ParticleType, ParticleMapping> particles = new Object2ObjectOpenHashMap<>();
        try {
            while (particlesIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = particlesIterator.next();
                JsonNode bedrockId = entry.getValue().get("bedrockId");
                JsonNode bedrockIdNumeric = entry.getValue().get("bedrockNumericId");
                JsonNode eventType = entry.getValue().get("eventType");
                particles.put(ParticleType.valueOf(entry.getKey().toUpperCase()), new ParticleMapping(
                        eventType == null ? null : LevelEventType.valueOf(eventType.asText().toUpperCase()),
                        bedrockId == null ? null : bedrockId.asText(),
                        bedrockIdNumeric == null ? -1 : bedrockIdNumeric.asInt())
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return particles;
    }
}