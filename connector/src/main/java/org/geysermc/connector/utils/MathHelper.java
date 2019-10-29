package org.geysermc.connector.utils;

/**
 * This file property the NukkitX project
 * https://github.com/NukkitX/Nukkit
 */
public class MathHelper {
    public static int log2(int bits) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(bits);
    }
}
