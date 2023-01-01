package org.geysermc.geyser.registry.mappings.util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.NonNull;

public record BlockPropertyTypeMaps(
    @NonNull Map<String, LinkedHashSet<String>> stringValuesMap, @NonNull Map<String, Map<String, String>> stateKeyStrings, 
    @NonNull Map<String, LinkedHashSet<Integer>> intValuesMap, @NonNull Map<String, Map<String, Integer>> stateKeyInts,
    @NonNull Set<String> booleanValuesSet, @NonNull Map<String, Map<String, Boolean>> stateKeyBools) {
}
