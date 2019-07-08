package org.geysermc.connector.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PluginYML {
    @JsonProperty("name")
    String name;

    @JsonProperty("version")
    String version;

    @JsonProperty("main")
    String main;
}
