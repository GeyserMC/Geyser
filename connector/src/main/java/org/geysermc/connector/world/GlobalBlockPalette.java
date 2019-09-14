package org.geysermc.connector.world;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapted from NukkitX: https://github.com/NukkitX/Nukkit
 */
public class GlobalBlockPalette {

    private static final Int2IntMap legacyToRuntimeId = new Int2IntOpenHashMap();
    private static final Int2IntMap runtimeIdToLegacy = new Int2IntOpenHashMap();
    private static final AtomicInteger runtimeIdAllocator = new AtomicInteger(0);

    static {
        legacyToRuntimeId.defaultReturnValue(-1);
        runtimeIdToLegacy.defaultReturnValue(-1);
    }

    public static int getOrCreateRuntimeId(int id, int meta) {
        return getOrCreateRuntimeId((id << 4) | meta);
    }

    public static int getOrCreateRuntimeId(int legacyId) throws NoSuchElementException {
        int runtimeId = legacyToRuntimeId.get(legacyId);
        if (!legacyToRuntimeId.containsKey(legacyId) || runtimeId == -1) {
            //runtimeId = registerMapping(runtimeIdAllocator.incrementAndGet(), legacyId);
           // throw new NoSuchElementException("Unmapped block registered id:" + (legacyId >>> 4) + " meta:" + (legacyId & 0xf));
            return 0;
        }
        return runtimeId;
    }

    public static int registerMapping(int legacyId) {
        int runtimeId = runtimeIdAllocator.getAndIncrement();
        runtimeIdToLegacy.put(runtimeId, legacyId);
        legacyToRuntimeId.put(legacyId, runtimeId);
        return runtimeId;
    }

    public static int getLegacyId(int runtimeId) {
        return runtimeIdToLegacy.get(runtimeId);
    }
}
