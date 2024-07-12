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

package org.geysermc.geyser.util;

import lombok.Getter;

import java.util.Locale;

/**
 * All interactive tags in enum form. For potential API usage.
 */
public enum InteractiveTag {
    NONE((Void) null),
    IGNITE_CREEPER("creeper"),
    EDIT,
    LEAVE_BOAT("exit.boat"),
    FEED,
    FISH("fishing"),
    MILK,
    MOOSHROOM_SHEAR("mooshear"),
    MOOSHROOM_MILK_STEW("moostew"),
    BOARD_BOAT("ride.boat"),
    RIDE_MINECART("ride.minecart"),
    RIDE_HORSE("ride.horse"),
    RIDE_STRIDER("ride.strider"),
    SHEAR,
    SIT,
    STAND,
    TALK,
    TAME,
    DYE,
    CURE,
    OPEN_CONTAINER("opencontainer"),
    CREATE_MAP("createMap"),
    TAKE_PICTURE("takepicture"),
    SADDLE,
    MOUNT,
    BOOST,
    WRITE,
    LEASH,
    REMOVE_LEASH("unleash"),
    NAME,
    ATTACH_CHEST("attachchest"),
    TRADE,
    POSE_ARMOR_STAND("armorstand.pose"),
    EQUIP_ARMOR_STAND("armorstand.equip"),
    READ,
    WAKE_VILLAGER("wakevillager"),
    BARTER,
    GIVE_ITEM_TO_ALLAY("allay"),
    EQUIP_WOLF_ARMOR("equipwolfarmor"),
    REMOVE_WOLF_ARMOR("removewolfarmor"),
    REPAIR_WOLF_ARMOR("repairwolfarmor");

    /**
     * The full string that should be passed on to the client.
     */
    @Getter
    private final String value;

    InteractiveTag(Void isNone) {
        this.value = "";
    }

    InteractiveTag(String value) {
        this.value = "action.interact." + value;
    }

    InteractiveTag() {
        this.value = "action.interact." + name().toLowerCase(Locale.ROOT);
    }
}
