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

/**
 * All interactive tags in enum form. For potential API usage.
 */
public enum InteractiveTag {
    NONE((Void) null),
    READ("action.interact.read"),
    WRITE("action.interact.write"),
    SADDLE("action.interact.saddle"),
    SCRAPE("action.interact.scrape"),
    RIDE_STRIDER("action.interact.ride.strider"),
    EQUIPNAUTILUSARMOR("action.interact.equipnautilusarmor"),
    EQUIPWOLFARMOR("action.interact.equipwolfarmor"),
    SHEAR("action.interact.shear"),
    EQUIPHARNESS("action.interact.equipharness"),
    FEED("action.interact.feed"),
    RIDE_BOAT("action.interact.ride.boat"),
    WAX_OFF("action.interact.wax_off"),
    REPAIRWOLFARMOR("action.interact.repairwolfarmor"),
    ARMORSTAND_EQUIP("action.interact.armorstand.equip"),
    RIDE_BOAT_TOUCH("action.interact.ride.boat.touch"),
    REMOVECARPET("action.interact.removecarpet"),
    SIT("action.interact.sit"),
    TRADE("action.interact.trade"),
    USE("action.interact.use"),
    NAME("action.interact.name"),
    RIDE_STRIDER_TOUCH("action.interact.ride.strider.touch"),
    WAKEVILLAGER("action.interact.wakevillager"),
    REMOVEHARNESS("action.interact.removeharness"),
    GIVE("action.interact.give"),
    GIVE_SULFUR_CUBE("action.interact.give_sulfur_cube"),
    EDIT("action.interact.edit"),
    WAX_ON("action.interact.wax_on"),
    MOUNT_TOUCH("action.interact.mount.touch"),
    REPAIR("action.interact.repair"),
    BRUSH("action.interact.brush"),
    EQUIPHORSEARMOR("action.interact.equiphorsearmor"),
    EXIT_RAFT("action.interact.exit.raft"),
    UNLEASH("action.interact.unleash"),
    MOOSTEW("action.interact.moostew"),
    ALLAY("action.interact.allay"),
    RETRIEVE("action.interact.retrieve"),
    MILK("action.interact.milk"),
    BOOST("action.interact.boost"),
    DROP_ITEM("action.interact.drop_item"),
    FISHING("action.interact.fishing"),
    ATTACHCHEST("action.interact.attachchest"),
    BARTER("action.interact.barter"),
    EXIT_BOAT("action.interact.exit.boat"),
    EQUIPCARPET("action.interact.equipcarpet"),
    CUT("action.interact.cut"),
    RIDE_HORSE_TOUCH("action.interact.ride.horse.touch"),
    DYE("action.interact.dye"),
    CREEPER("action.interact.creeper"),
    REMOVEHORSEARMOR("action.interact.removehorsearmor"),
    TAKEPICTURE("action.interact.takepicture"),
    REMOVENAUTILUSARMOR("action.interact.removenautilusarmor"),
    MOUNT("action.interact.mount"),
    ARMORSTAND_POSE("action.interact.armorstand.pose"),
    RIDE_MINECART_TOUCH("action.interact.ride.minecart.touch"),
    CREATEMAP("action.interact.createMap"),
    OPENCONTAINER("action.interact.opencontainer"),
    CURE("action.interact.cure"),
    STAND("action.interact.stand"),
    REMOVEWOLFARMOR("action.interact.removewolfarmor"),
    MOOSHEAR("action.interact.mooshear"),
    RIDE_HORSE("action.interact.ride.horse"),
    LEASH("action.interact.leash"),
    SPAWN_EGG_BABY("action.interact.spawn_egg_baby"),
    RIDE_MINECART("action.interact.ride.minecart"),
    REMOVESADDLE("action.interact.removesaddle"),
    RIDE_CUSHION("action.interact.ride.cushion"),
    TALK("action.interact.talk"),
    TAME("action.interact.tame"),
    TAKE_SULFUR_CUBE("action.interact.take_sulfur_cube");


    /**
     * The full string that should be passed on to the client.
     */
    @Getter
    private final String value;

    InteractiveTag(Void isNone) {
        this.value = "";
    }

    InteractiveTag(String value) {
        this.value = value;
    }
}
