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

package org.geysermc.geyser.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelParticlesPacket;

import java.util.Random;

public record ParticleRandomization(Vector3f position, double xAux, double yAux, double zAux) {

    public static ParticleRandomization ofStatic(Vector3f position) {
        return new ParticleRandomization(position, 0.0, 0.0, 0.0);
    }

    public static ParticleRandomization ofPacket(ClientboundLevelParticlesPacket packet, Random random) {
        // Mirrors https://mcsrc.dev/2/26.4-snapshot-8/net/minecraft/client/multiplayer/ClientPacketListener#L2272-2310
        if (packet.getAmount() == 0) {
            return new ParticleRandomization(
                Vector3f.from(packet.getX(), packet.getY(), packet.getZ()),
                packet.getXMaxSpeed() * packet.getOffsetX(),
                packet.getYMaxSpeed() * packet.getOffsetY(),
                packet.getZMaxSpeed() * packet.getOffsetZ()
            );
        }
        ClientboundLevelParticlesPacket.RandomizationType randomizationType = packet.getRandomizationType();

        double xDist;
        double yDist;
        double zDist;
        double xAux;
        double yAux;
        double zAux;
        if (randomizationType == ClientboundLevelParticlesPacket.RandomizationType.ALTERNATIVE
            || randomizationType == ClientboundLevelParticlesPacket.RandomizationType.ALTERNATIVE_WITH_SPEED) {
            xDist = random.nextDouble() * packet.getOffsetX();
            yDist = random.nextDouble() * packet.getOffsetY();
            zDist = random.nextDouble() * packet.getOffsetZ();
            xAux = packet.getXMaxSpeed();
            yAux = packet.getYMaxSpeed();
            zAux = packet.getZMaxSpeed();
            if (randomizationType == ClientboundLevelParticlesPacket.RandomizationType.ALTERNATIVE_WITH_SPEED) {
                xAux *= random.nextDouble();
                yAux *= random.nextDouble();
                zAux *= random.nextDouble();
            }
        } else {
            xDist = random.nextGaussian() * packet.getOffsetX();
            yDist = random.nextGaussian() * packet.getOffsetY();
            zDist = random.nextGaussian() * packet.getOffsetZ();
            xAux = random.nextGaussian() * packet.getXMaxSpeed();
            yAux = random.nextGaussian() * packet.getYMaxSpeed();
            zAux = random.nextGaussian() * packet.getZMaxSpeed();
        }

        return new ParticleRandomization(
            Vector3f.from(packet.getX() + xDist, packet.getY() + yDist, packet.getZ() + zDist),
            xAux, yAux, zAux
        );
    }
}
