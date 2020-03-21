package org.geysermc.connector.network.translators.item;

import lombok.Getter;

@Getter
public class ToolItemEntry extends ItemEntry {
    private final String toolType;
    private final String toolTier;

    public ToolItemEntry(String javaIdentifier, int javaId, int bedrockId, int bedrockData, String toolType, String toolTier) {
        super(javaIdentifier, javaId, bedrockId, bedrockData);
        this.toolType = toolType;
        this.toolTier = toolTier;
    }
}
