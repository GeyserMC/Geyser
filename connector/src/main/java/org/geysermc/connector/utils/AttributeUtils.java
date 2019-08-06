package org.geysermc.connector.utils;

import org.geysermc.connector.entity.attribute.Attribute;
import org.geysermc.connector.entity.attribute.AttributeType;

public class AttributeUtils {

    public static com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute getJavaAttribute(Attribute attribute) {
        if (!attribute.getType().isJavaAttribute())
            return null;

        com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType type = null;
        try {
            type = com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType.valueOf("GENERIC_" + attribute.getType().name());
        } catch (Exception ex) {
            // Catch and loop since attributes can semi-overlap
            for (AttributeType attributeType : AttributeType.values()) {
                if (!attributeType.isJavaAttribute())
                    continue;

                if (!attributeType.getJavaIdentifier().equals(attribute.getType().getJavaIdentifier()))
                    continue;

                try {
                    type = com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType.valueOf("GENERIC_" + attributeType.name());
                } catch (Exception e) {
                    continue;
                }
            }
        }

        if (type == null)
            return null;

        return new com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute(type, attribute.getValue());
    }

    public static com.nukkitx.protocol.bedrock.data.Attribute getBedrockAttribute(Attribute attribute) {
        AttributeType type = attribute.getType();
        if (!type.isBedrockAttribute())
            return null;

        return new com.nukkitx.protocol.bedrock.data.Attribute(type.getBedrockIdentifier(), attribute.getMinimum(), attribute.getMaximum(), attribute.getValue(), attribute.getDefaultValue());
    }
}
