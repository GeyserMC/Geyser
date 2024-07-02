/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.sound.block;

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.sound.BlockSoundInteractionTranslator;
import org.geysermc.geyser.translator.sound.SoundTranslator;
import org.geysermc.geyser.util.SoundUtils;

@SoundTranslator(blocks = {"fence_gate"})
public class FenceGateSoundInteractionTranslator implements BlockSoundInteractionTranslator {
    @Override
    public void translate(GeyserSession session, Vector3f position, String identifier) {
        boolean open = identifier.contains("open=true");
        String materialIdentifier = getMaterialIdentifier(identifier);
        float volume = 1.0f;
        float pitch = 1.0f;
        // Sounds are quieter for wooden and bamboo fence gates.
        if (materialIdentifier.equals("block.") || materialIdentifier.equals("block.bamboo_wood_")) {
            volume = 0.9f;
        }
        if (materialIdentifier.equals("block.bamboo_wood_")) {
            pitch = 1.1f;
        }
        String status = open ? "fence_gate.open" : "fence_gate.close";
        SoundUtils.playSound(session, "minecraft:" + materialIdentifier + status, position, volume, pitch);
    }

    private static String getMaterialIdentifier(String identifier) {
        String type = "block.";
        if (identifier.contains("bamboo_")) {
            type = "block.bamboo_wood_";
        } else if (identifier.contains("cherry_")) {
            type = "block.cherry_wood_";
        } else if (identifier.contains("crimson_") || identifier.contains("warped_")) {
            type = "block.nether_wood_";
        }
        return type;
    }
}
