/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.mcprotocollib.protocol.data.game.level.map.MapIconType;

public enum BedrockMapIcon {
    ICON_WHITE_ARROW(MapIconType.PLAYER, 0),
    ICON_ITEM_FRAME(MapIconType.FRAME, 7),
    ICON_RED_ARROW(MapIconType.RED_MARKER, 2),
    ICON_BLUE_ARROW(MapIconType.BLUE_MARKER, 3),
    ICON_WHITE_CROSS(MapIconType.TARGET_X, 4, 0, 0, 0), // Doesn't exist on Bedrock, replaced with a black cross
    ICON_RED_POINTER(MapIconType.TARGET_POINT, 5),
    ICON_WHITE_CIRCLE(MapIconType.PLAYER_OFF_MAP, 6),
    ICON_SMALL_WHITE_CIRCLE(MapIconType.PLAYER_OFF_LIMITS, 13),
    ICON_MANSION(MapIconType.MANSION, 14),
    ICON_TEMPLE(MapIconType.MONUMENT, 15),
    ICON_WHITE_BANNER(MapIconType.BANNER_WHITE, 13, 255, 255, 255),
    ICON_ORANGE_BANNER(MapIconType.BANNER_ORANGE, 13, 249, 128, 29),
    ICON_MAGENTA_BANNER(MapIconType.BANNER_MAGENTA, 13, 199, 78, 189),
    ICON_LIGHT_BLUE_BANNER(MapIconType.BANNER_LIGHT_BLUE, 13, 58, 179, 218),
    ICON_YELLOW_BANNER(MapIconType.BANNER_YELLOW, 13, 254, 216, 61),
    ICON_LIME_BANNER(MapIconType.BANNER_LIME, 13, 128, 199, 31),
    ICON_PINK_BANNER(MapIconType.BANNER_PINK, 13, 243, 139, 170),
    ICON_GRAY_BANNER(MapIconType.BANNER_GRAY, 13, 71, 79, 82),
    ICON_LIGHT_GRAY_BANNER(MapIconType.BANNER_LIGHT_GRAY, 13, 157, 157, 151),
    ICON_CYAN_BANNER(MapIconType.BANNER_CYAN, 13, 22, 156, 156),
    ICON_PURPLE_BANNER(MapIconType.BANNER_PURPLE, 13, 137, 50, 184),
    ICON_BLUE_BANNER(MapIconType.BANNER_BLUE, 13, 60, 68, 170),
    ICON_BROWN_BANNER(MapIconType.BANNER_BROWN, 13, 131, 84, 50),
    ICON_GREEN_BANNER(MapIconType.BANNER_GREEN, 13, 94, 124, 22),
    ICON_RED_BANNER(MapIconType.BANNER_RED, 13, 176, 46, 38),
    ICON_BLACK_BANNER(MapIconType.BANNER_BLACK, 13, 29, 29, 33),
    ICON_TREASURE_MARKER(MapIconType.RED_X, 4),
    ICON_DESERT_VILLAGE(MapIconType.VILLAGE_DESERT, 17),
    ICON_PLAINS_VILLAGE(MapIconType.VILLAGE_PLAINS, 18),
    ICON_SAVANNA_VILLAGE(MapIconType.VILLAGE_SAVANNA, 19),
    ICON_SNOWY_VILLAGE(MapIconType.VILLAGE_SNOWY, 20),
    ICON_TAIGA_VILLAGE(MapIconType.VILLAGE_TAIGA, 21),
    ICON_JUNGLE_TEMPLE(MapIconType.JUNGLE_TEMPLE, 22),
    ICON_SWAMP_HUT(MapIconType.SWAMP_HUT, 23),
    ICON_TRIAL_CHAMBERS(MapIconType.TRIAL_CHAMBERS, 24),
    ICON_ABANDONED_CAMP(MapIconType.ABANDONED_CAMP, 25),
    ICON_ANCIENT_CITY(MapIconType.ANCIENT_CITY, 26),
    ICON_DESERT_PYRAMID(MapIconType.DESERT_PYRAMID, 28),
    ICON_MINESHAFT(MapIconType.MINESHAFT, 27),
    ICON_OCEAN_RUIN_WARM(MapIconType.OCEAN_RUIN_WARM, 29);

    private static final BedrockMapIcon[] VALUES = values();

    private final MapIconType iconType;

    @Getter
    private final int iconID;

    private final int red;
    private final int green;
    private final int blue;

    BedrockMapIcon(MapIconType iconType, int iconID) {
        this(iconType, iconID, 255, 255, 255);
    }

    BedrockMapIcon(MapIconType iconType, int iconID, int red, int green, int blue) {
        this.iconType = iconType;
        this.iconID = iconID;

        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /**
     * Get the BedrockMapIcon for the Java MapIconType
     *
     * @param iconType A MapIconType
     * @return The mapping for a BedrockMapIcon
     */
    public static @Nullable BedrockMapIcon fromType(MapIconType iconType) {
        for (BedrockMapIcon icon : VALUES) {
            if (icon.iconType.equals(iconType)) {
                return icon;
            }
        }

        return null;
    }

    /**
     * Get the ARGB value of a BedrockMapIcon
     *
     * @return ARGB as an int
     */
    public int toARGB() {
        final int alpha = 255;

        return ((alpha & 0xFF) << 24) |
                ((red & 0xFF) << 16) |
                ((green & 0xFF) << 8) |
                (blue & 0xFF);
    }
}
