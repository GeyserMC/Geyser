package org.geysermc.connector.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum PaintingType {
    KEBAB("Kebab", 1, 1),
    AZTEC("Aztec", 1, 1),
    ALBAN("Alban", 1, 1),
    AZTEC2("Aztec2", 1, 1),
    BOMB("Bomb", 1, 1),
    PLANT("Plant", 1, 1),
    WASTELAND("Wasteland", 1, 1),
    WANDERER("Wanderer", 1, 2),
    GRAHAM("Graham", 1, 2),
    POOL("Pool", 2, 1),
    COURBET("Courbet", 2, 1),
    SUNSET("Sunset", 2, 1),
    SEA("Sea", 2, 1),
    CREEBET("Creebet", 2, 1),
    MATCH("Match", 2, 2),
    BUST("Bust", 2, 2),
    STAGE("Stage", 2, 2),
    VOID("Void", 2, 2),
    SKULL_AND_ROSES("SkullAndRoses", 2, 2),
    WITHER("Wither", 2, 2),
    FIGHTERS("Fighters", 4, 2),
    SKELETON("Skeleton", 4, 3),
    DONKEY_KONG("DonkeyKong", 4, 3),
    POINTER("Pointer", 4, 4),
    PIG_SCENE("Pigscene", 4, 4),
    BURNING_SKULL("Flaming Skull", 4, 4); // burning skull on java edition, flaming skull on bedrock

    private static final PaintingType[] VALUES = values();
    private String bedrockName;
    private int width;
    private int height;

    public com.github.steveice10.mc.protocol.data.game.entity.type.PaintingType toJavaType() {
        return com.github.steveice10.mc.protocol.data.game.entity.type.PaintingType.valueOf(name());
    }

    public static PaintingType getByName(String javaName) {
        for (PaintingType paintingName : VALUES) {
            if (paintingName.name().equalsIgnoreCase(javaName)) return paintingName;
        }
        return KEBAB;
    }

    public static PaintingType getByPaintingType(com.github.steveice10.mc.protocol.data.game.entity.type.PaintingType paintingType) {
        return getByName(paintingType.name());
    }
}
