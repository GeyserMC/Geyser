package org.geysermc.api.plugin;

import lombok.Getter;
import lombok.Setter;

public class Plugin {

    @Getter
    @Setter
    private boolean enabled = true;

    /**
     * Called when a plugin is enabled
     */
    public void onEnable() {

    }

    /**
     * Called when a plugin is disabled
     */
    public void onDisable() {

    }

    /**
     * Called when a plugin is loaded
     */
    public void onLoad() {

    }
}
