package org.geysermc.connector.utils;

public class MathUtils {

    public static int ceil(float floatNumber) {
        int truncated = (int) floatNumber;
        return floatNumber > truncated ? truncated + 1 : truncated;
    }
}
