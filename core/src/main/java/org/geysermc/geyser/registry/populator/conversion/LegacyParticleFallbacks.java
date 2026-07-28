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

package org.geysermc.geyser.registry.populator.conversion;

import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEventType;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;

/**
 * Omits particles / level events that legacy Bedrock clients cannot safely render.
 */
public final class LegacyParticleFallbacks {

    private LegacyParticleFallbacks() {
    }

    public static boolean shouldOmitParticle(ParticleType type, int protocolVersion) {
        return switch (type) {
            // Tricky Trials (1.21.0)
            case GUST, SMALL_GUST, GUST_EMITTER_LARGE, GUST_EMITTER_SMALL,
                 OMINOUS_SPAWNING, TRIAL_OMEN, TRIAL_SPAWNER_DETECTED_PLAYER,
                 TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS -> !GameProtocol.is1_21_0orHigher(protocolVersion);
            // Pale Garden / creaking (1.21.50)
            case TRAIL, PALE_OAK_LEAVES, BLOCK_CRUMBLE -> !GameProtocol.is1_21_50orHigher(protocolVersion);
            // Firefly bush era (~1.21.70+)
            case FIREFLY -> !GameProtocol.is1_21_80orHigher(protocolVersion);
            // Tinted leaves particle (modern leaves color path)
            case TINTED_LEAVES -> !GameProtocol.is1_21_90orHigher(protocolVersion);
            // Sulfur cube (26.10)
            case SULFUR_BUBBLES, SULFUR_CUBE_GOO -> !GameProtocol.is26_10orHigher(protocolVersion);
            default -> false;
        };
    }

    public static boolean shouldOmitParticleIdentifier(String identifier, int protocolVersion) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }
        String id = identifier.toLowerCase();
        if (id.contains("trial") || id.contains("ominous") || id.contains("gust")) {
            return !GameProtocol.is1_21_0orHigher(protocolVersion);
        }
        if (id.contains("creaking") || id.contains("pale_oak")) {
            return !GameProtocol.is1_21_50orHigher(protocolVersion);
        }
        if (id.contains("firefly")) {
            return !GameProtocol.is1_21_80orHigher(protocolVersion);
        }
        if (id.contains("biome_tinted_leaves") || id.contains("tinted_leaves")) {
            return !GameProtocol.is1_21_90orHigher(protocolVersion);
        }
        if (id.contains("sulfur")) {
            return !GameProtocol.is26_10orHigher(protocolVersion);
        }
        return false;
    }

    public static boolean shouldOmitLevelEvent(LevelEventType event, int protocolVersion) {
        return switch (event) {
            case ANIMATION_VAULT_ACTIVATE, ANIMATION_VAULT_DEACTIVATE, ANIMATION_VAULT_EJECT_ITEM,
                 ANIMATION_TRIAL_SPAWNER_EJECT_ITEM,
                 PARTICLES_TRIAL_SPAWNER_DETECT_PLAYER, PARTICLES_TRIAL_SPAWNER_DETECT_PLAYER_OMINOUS,
                 PARTICLES_TRIAL_SPAWNER_BECOME_OMINOUS, PARTICLES_TRIAL_SPAWNER_SPAWN_MOB_AT,
                 PARTICLES_TRIAL_SPAWNER_SPAWN, PARTICLES_TRIAL_SPAWNER_SPAWN_ITEM ->
                !GameProtocol.is1_21_0orHigher(protocolVersion);
            default -> false;
        };
    }

    public static boolean shouldOmitCreakingBeam(int protocolVersion) {
        return !GameProtocol.is1_21_50orHigher(protocolVersion);
    }
}
