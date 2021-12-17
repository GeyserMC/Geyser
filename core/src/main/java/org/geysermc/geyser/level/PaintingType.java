/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.level;

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
    BURNING_SKULL("BurningSkull", 4, 4);

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
