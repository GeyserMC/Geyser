package org.geysermc.api;

import org.geysermc.api.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class Geyser {
    private static final List<Plugin> plugins = new ArrayList<>();

    public static List<Plugin> getPlugins() {
        return new ArrayList<>(plugins);
    }

    public static void add(Plugin p) {
        plugins.add(p);
    }
}
