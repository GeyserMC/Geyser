package org.geysermc.connector.network.translators.item;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

@Getter
public class ToolItemEntry extends ItemEntry {
    private final String toolType;
    private final String toolTier;

    public ToolItemEntry(String javaIdentifier, int javaId, int bedrockId, int bedrockData, String toolType, String toolTier, boolean isBlock, JsonNode extra) {
        super(javaIdentifier, javaId, bedrockId, bedrockData, isBlock, extra);
        this.toolType = toolType;
        this.toolTier = toolTier;
    }
}
