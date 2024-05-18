/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.block.property;

import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.Direction;

public final class Properties {
    public static final Property<Boolean> ATTACHED = Property.create("attached");
    public static final Property<Boolean> BOTTOM = Property.create("bottom");
    public static final Property<Boolean> CONDITIONAL = Property.create("conditional");
    public static final Property<Boolean> DISARMED = Property.create("disarmed");
    public static final Property<Boolean> DRAG = Property.create("drag");
    public static final Property<Boolean> ENABLED = Property.create("enabled");
    public static final Property<Boolean> EXTENDED = Property.create("extended");
    public static final Property<Boolean> EYE = Property.create("eye");
    public static final Property<Boolean> FALLING = Property.create("falling");
    public static final Property<Boolean> HANGING = Property.create("hanging");
    public static final Property<Boolean> HAS_BOTTLE_0 = Property.create("has_bottle_0");
    public static final Property<Boolean> HAS_BOTTLE_1 = Property.create("has_bottle_1");
    public static final Property<Boolean> HAS_BOTTLE_2 = Property.create("has_bottle_2");
    public static final Property<Boolean> HAS_RECORD = Property.create("has_record");
    public static final Property<Boolean> HAS_BOOK = Property.create("has_book");
    public static final Property<Boolean> INVERTED = Property.create("inverted");
    public static final Property<Boolean> IN_WALL = Property.create("in_wall");
    public static final Property<Boolean> LIT = Property.create("lit");
    public static final Property<Boolean> LOCKED = Property.create("locked");
    public static final Property<Boolean> OCCUPIED = Property.create("occupied");
    public static final Property<Boolean> OPEN = Property.create("open");
    public static final Property<Boolean> PERSISTENT = Property.create("persistent");
    public static final Property<Boolean> POWERED = Property.create("powered");
    public static final Property<Boolean> SHORT = Property.create("short");
    public static final Property<Boolean> SIGNAL_FIRE = Property.create("signal_fire");
    public static final Property<Boolean> SNOWY = Property.create("snowy");
    public static final Property<Boolean> TRIGGERED = Property.create("triggered");
    public static final Property<Boolean> UNSTABLE = Property.create("unstable");
    public static final Property<Boolean> WATERLOGGED = Property.create("waterlogged");
    public static final Property<Boolean> BERRIES = Property.create("berries");
    public static final Property<Boolean> BLOOM = Property.create("bloom");
    public static final Property<Boolean> SHRIEKING = Property.create("shrieking");
    public static final Property<Boolean> CAN_SUMMON = Property.create("can_summon");
    public static final Property<Axis> HORIZONTAL_AXIS = Property.create("axis");
    public static final Property<Axis> AXIS = Property.create("axis");
    public static final Property<Boolean> UP = Property.create("up");
    public static final Property<Boolean> DOWN = Property.create("down");
    public static final Property<Boolean> NORTH = Property.create("north");
    public static final Property<Boolean> EAST = Property.create("east");
    public static final Property<Boolean> SOUTH = Property.create("south");
    public static final Property<Boolean> WEST = Property.create("west");
    public static final Property<Direction> FACING = Property.create("facing");
    public static final Property<Direction> FACING_HOPPER = Property.create("facing");
    public static final Property<Direction> HORIZONTAL_FACING = Property.create("facing");
    public static final Property<Integer> FLOWER_AMOUNT = Property.create("flower_amount");
    public static final Property<String> ORIENTATION = Property.create("orientation");
    public static final Property<String> ATTACH_FACE = Property.create("face");
    public static final Property<String> BELL_ATTACHMENT = Property.create("attachment");
    public static final Property<String> EAST_WALL = Property.create("east");
    public static final Property<String> NORTH_WALL = Property.create("north");
    public static final Property<String> SOUTH_WALL = Property.create("south");
    public static final Property<String> WEST_WALL = Property.create("west");
    public static final Property<String> EAST_REDSTONE = Property.create("east");
    public static final Property<String> NORTH_REDSTONE = Property.create("north");
    public static final Property<String> SOUTH_REDSTONE = Property.create("south");
    public static final Property<String> WEST_REDSTONE = Property.create("west");
    public static final Property<String> DOUBLE_BLOCK_HALF = Property.create("half");
    public static final Property<String> HALF = Property.create("half");
    public static final Property<String> RAIL_SHAPE = Property.create("shape");
    public static final Property<String> RAIL_SHAPE_STRAIGHT = Property.create("shape");
    public static final Property<Integer> AGE_1 = Property.create("age");
    public static final Property<Integer> AGE_2 = Property.create("age");
    public static final Property<Integer> AGE_3 = Property.create("age");
    public static final Property<Integer> AGE_4 = Property.create("age");
    public static final Property<Integer> AGE_5 = Property.create("age");
    public static final Property<Integer> AGE_7 = Property.create("age");
    public static final Property<Integer> AGE_15 = Property.create("age");
    public static final Property<Integer> AGE_25 = Property.create("age");
    public static final Property<Integer> BITES = Property.create("bites");
    public static final Property<Integer> CANDLES = Property.create("candles");
    public static final Property<Integer> DELAY = Property.create("delay");
    public static final Property<Integer> DISTANCE = Property.create("distance");
    public static final Property<Integer> EGGS = Property.create("eggs");
    public static final Property<Integer> HATCH = Property.create("hatch");
    public static final Property<Integer> LAYERS = Property.create("layers");
    public static final Property<Integer> LEVEL_CAULDRON = Property.create("level");
    public static final Property<Integer> LEVEL_COMPOSTER = Property.create("level");
    public static final Property<Integer> LEVEL_FLOWING = Property.create("level");
    public static final Property<Integer> LEVEL_HONEY = Property.create("honey_level");
    public static final Property<Integer> LEVEL = Property.create("level");
    public static final Property<Integer> MOISTURE = Property.create("moisture");
    public static final Property<Integer> NOTE = Property.create("note");
    public static final Property<Integer> PICKLES = Property.create("pickles");
    public static final Property<Integer> POWER = Property.create("power");
    public static final Property<Integer> STAGE = Property.create("stage");
    public static final Property<Integer> STABILITY_DISTANCE = Property.create("distance");
    public static final Property<Integer> RESPAWN_ANCHOR_CHARGES = Property.create("charges");
    public static final Property<Integer> ROTATION_16 = Property.create("rotation");
    public static final Property<String> BED_PART = Property.create("part");
    public static final Property<ChestType> CHEST_TYPE = Property.create("type");
    public static final Property<String> MODE_COMPARATOR = Property.create("mode");
    public static final Property<String> DOOR_HINGE = Property.create("hinge");
    public static final Property<String> NOTEBLOCK_INSTRUMENT = Property.create("instrument");
    public static final Property<String> PISTON_TYPE = Property.create("type");
    public static final Property<String> SLAB_TYPE = Property.create("type");
    public static final Property<String> STAIRS_SHAPE = Property.create("shape");
    public static final Property<String> STRUCTUREBLOCK_MODE = Property.create("mode");
    public static final Property<String> BAMBOO_LEAVES = Property.create("leaves");
    public static final Property<String> TILT = Property.create("tilt");
    public static final Property<Direction> VERTICAL_DIRECTION = Property.create("vertical_direction");
    public static final Property<String> DRIPSTONE_THICKNESS = Property.create("thickness");
    public static final Property<String> SCULK_SENSOR_PHASE = Property.create("sculk_sensor_phase");
    public static final Property<Boolean> CHISELED_BOOKSHELF_SLOT_0_OCCUPIED = Property.create("slot_0_occupied");
    public static final Property<Boolean> CHISELED_BOOKSHELF_SLOT_1_OCCUPIED = Property.create("slot_1_occupied");
    public static final Property<Boolean> CHISELED_BOOKSHELF_SLOT_2_OCCUPIED = Property.create("slot_2_occupied");
    public static final Property<Boolean> CHISELED_BOOKSHELF_SLOT_3_OCCUPIED = Property.create("slot_3_occupied");
    public static final Property<Boolean> CHISELED_BOOKSHELF_SLOT_4_OCCUPIED = Property.create("slot_4_occupied");
    public static final Property<Boolean> CHISELED_BOOKSHELF_SLOT_5_OCCUPIED = Property.create("slot_5_occupied");
    public static final Property<Integer> DUSTED = Property.create("dusted");
    public static final Property<Boolean> CRACKED = Property.create("cracked");
    public static final Property<Boolean> CRAFTING = Property.create("crafting");
    public static final Property<String> TRIAL_SPAWNER_STATE = Property.create("trial_spawner_state");
    public static final Property<String> VAULT_STATE = Property.create("vault_state");
    public static final Property<Boolean> OMINOUS = Property.create("ominous");
}
