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
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty ATTACHED = BooleanProperty.create("attached");
    public static final BooleanProperty BERRIES = BooleanProperty.create("berries");
    public static final BooleanProperty BLOOM = BooleanProperty.create("bloom");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final BooleanProperty CAN_SUMMON = BooleanProperty.create("can_summon");
    public static final BooleanProperty CONDITIONAL = BooleanProperty.create("conditional");
    public static final BooleanProperty DISARMED = BooleanProperty.create("disarmed");
    public static final BooleanProperty DRAG = BooleanProperty.create("drag");
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    public static final BooleanProperty EXTENDED = BooleanProperty.create("extended");
    public static final BooleanProperty EYE = BooleanProperty.create("eye");
    public static final BooleanProperty FALLING = BooleanProperty.create("falling");
    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
    public static final BooleanProperty HAS_BOTTLE_0 = BooleanProperty.create("has_bottle_0");
    public static final BooleanProperty HAS_BOTTLE_1 = BooleanProperty.create("has_bottle_1");
    public static final BooleanProperty HAS_BOTTLE_2 = BooleanProperty.create("has_bottle_2");
    public static final BooleanProperty HAS_RECORD = BooleanProperty.create("has_record");
    public static final BooleanProperty HAS_BOOK = BooleanProperty.create("has_book");
    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");
    public static final BooleanProperty IN_WALL = BooleanProperty.create("in_wall");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");
    public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty PERSISTENT = BooleanProperty.create("persistent");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty SHORT = BooleanProperty.create("short");
    public static final BooleanProperty SHRIEKING = BooleanProperty.create("shrieking");
    public static final BooleanProperty SIGNAL_FIRE = BooleanProperty.create("signal_fire");
    public static final BooleanProperty SNOWY = BooleanProperty.create("snowy");
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");
    public static final BooleanProperty UNSTABLE = BooleanProperty.create("unstable");
    public static final BooleanProperty WATERLOGGED = BooleanProperty.create("waterlogged");
    public static final EnumProperty<Axis> HORIZONTAL_AXIS = EnumProperty.create("axis", Axis.X, Axis.Z);
    public static final EnumProperty<Axis> AXIS = EnumProperty.create("axis", Axis.VALUES);
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN);
    public static final EnumProperty<Direction> FACING_HOPPER = EnumProperty.create("facing", Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    public static final EnumProperty<Direction> HORIZONTAL_FACING = EnumProperty.create("facing", Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    public static final IntegerProperty FLOWER_AMOUNT = IntegerProperty.create("flower_amount", 1, 4);
    public static final EnumProperty<FrontAndTop> ORIENTATION = EnumProperty.create("orientation", FrontAndTop.VALUES);
    public static final BasicEnumProperty ATTACH_FACE = BasicEnumProperty.create("face", "floor", "wall", "ceiling");
    public static final BasicEnumProperty BELL_ATTACHMENT = BasicEnumProperty.create("attachment", "floor", "ceiling", "single_wall", "double_wall");
    public static final BasicEnumProperty EAST_WALL = BasicEnumProperty.create("east", "none", "low", "tall");
    public static final BasicEnumProperty NORTH_WALL = BasicEnumProperty.create("north", "none", "low", "tall");
    public static final BasicEnumProperty SOUTH_WALL = BasicEnumProperty.create("south", "none", "low", "tall");
    public static final BasicEnumProperty WEST_WALL = BasicEnumProperty.create("west", "none", "low", "tall");
    public static final BasicEnumProperty EAST_REDSTONE = BasicEnumProperty.create("east", "up", "side", "none");
    public static final BasicEnumProperty NORTH_REDSTONE = BasicEnumProperty.create("north", "up", "side", "none");
    public static final BasicEnumProperty SOUTH_REDSTONE = BasicEnumProperty.create("south", "up", "side", "none");
    public static final BasicEnumProperty WEST_REDSTONE = BasicEnumProperty.create("west", "up", "side", "none");
    public static final BasicEnumProperty DOUBLE_BLOCK_HALF = BasicEnumProperty.create("half", "upper", "lower");
    public static final BasicEnumProperty HALF = BasicEnumProperty.create("half", "top", "bottom");
    public static final BasicEnumProperty RAIL_SHAPE = BasicEnumProperty.create("shape", "north_south", "east_west", "ascending_east", "ascending_west", "ascending_north", "ascending_south", "south_east", "south_west", "north_west", "north_east");
    public static final BasicEnumProperty RAIL_SHAPE_STRAIGHT = BasicEnumProperty.create("shape", "north_south", "east_west", "ascending_east", "ascending_west", "ascending_north", "ascending_south");
    public static final IntegerProperty AGE_1 = IntegerProperty.create("age", 0, 1);
    public static final IntegerProperty AGE_2 = IntegerProperty.create("age", 0, 2);
    public static final IntegerProperty AGE_3 = IntegerProperty.create("age", 0, 3);
    public static final IntegerProperty AGE_4 = IntegerProperty.create("age", 0, 4);
    public static final IntegerProperty AGE_5 = IntegerProperty.create("age", 0, 5);
    public static final IntegerProperty AGE_7 = IntegerProperty.create("age", 0, 7);
    public static final IntegerProperty AGE_15 = IntegerProperty.create("age", 0, 15);
    public static final IntegerProperty AGE_25 = IntegerProperty.create("age", 0, 25);
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 6);
    public static final IntegerProperty CANDLES = IntegerProperty.create("candles", 1, 4);
    public static final IntegerProperty DELAY = IntegerProperty.create("delay", 1, 4);
    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 1, 7);
    public static final IntegerProperty EGGS = IntegerProperty.create("eggs", 1, 4);
    public static final IntegerProperty HATCH = IntegerProperty.create("hatch", 0, 2);
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 8);
    public static final IntegerProperty LEVEL_CAULDRON = IntegerProperty.create("level", 1, 3);
    public static final IntegerProperty LEVEL_COMPOSTER = IntegerProperty.create("level", 0, 8);
    public static final IntegerProperty LEVEL_FLOWING = IntegerProperty.create("level", 1, 8);
    public static final IntegerProperty LEVEL_HONEY = IntegerProperty.create("honey_level", 0, 5);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 15);
    public static final IntegerProperty MOISTURE = IntegerProperty.create("moisture", 0, 7);
    public static final IntegerProperty NOTE = IntegerProperty.create("note", 0, 24);
    public static final IntegerProperty PICKLES = IntegerProperty.create("pickles", 1, 4);
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 1);
    public static final IntegerProperty STABILITY_DISTANCE = IntegerProperty.create("distance", 0, 7);
    public static final IntegerProperty RESPAWN_ANCHOR_CHARGES = IntegerProperty.create("charges", 0, 4);
    public static final IntegerProperty ROTATION_16 = IntegerProperty.create("rotation", 0, 15);
    public static final BasicEnumProperty BED_PART = BasicEnumProperty.create("part", "head", "foot");
    public static final EnumProperty<ChestType> CHEST_TYPE = EnumProperty.create("type", ChestType.VALUES);
    public static final BasicEnumProperty MODE_COMPARATOR = BasicEnumProperty.create("mode", "compare", "subtract");
    public static final BasicEnumProperty DOOR_HINGE = BasicEnumProperty.create("hinge", "left", "right");
    public static final BasicEnumProperty NOTEBLOCK_INSTRUMENT = BasicEnumProperty.create("instrument", "harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling", "zombie", "skeleton", "creeper", "dragon", "wither_skeleton", "piglin", "custom_head");
    public static final BasicEnumProperty PISTON_TYPE = BasicEnumProperty.create("type", "normal", "sticky");
    public static final BasicEnumProperty SLAB_TYPE = BasicEnumProperty.create("type", "top", "bottom", "double");
    public static final BasicEnumProperty STAIRS_SHAPE = BasicEnumProperty.create("shape", "straight", "inner_left", "inner_right", "outer_left", "outer_right");
    public static final BasicEnumProperty STRUCTUREBLOCK_MODE = BasicEnumProperty.create("mode", "save", "load", "corner", "data");
    public static final BasicEnumProperty BAMBOO_LEAVES = BasicEnumProperty.create("leaves", "none", "small", "large");
    public static final BasicEnumProperty TILT = BasicEnumProperty.create("tilt", "none", "unstable", "partial", "full");
    public static final EnumProperty<Direction> VERTICAL_DIRECTION = EnumProperty.create("vertical_direction", Direction.UP, Direction.DOWN);
    public static final BasicEnumProperty DRIPSTONE_THICKNESS = BasicEnumProperty.create("thickness", "tip_merge", "tip", "frustum", "middle", "base");
    public static final BasicEnumProperty SCULK_SENSOR_PHASE = BasicEnumProperty.create("sculk_sensor_phase", "inactive", "active", "cooldown");
    public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_0_OCCUPIED = BooleanProperty.create("slot_0_occupied");
    public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_1_OCCUPIED = BooleanProperty.create("slot_1_occupied");
    public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_2_OCCUPIED = BooleanProperty.create("slot_2_occupied");
    public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_3_OCCUPIED = BooleanProperty.create("slot_3_occupied");
    public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_4_OCCUPIED = BooleanProperty.create("slot_4_occupied");
    public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_5_OCCUPIED = BooleanProperty.create("slot_5_occupied");
    public static final IntegerProperty DUSTED = IntegerProperty.create("dusted", 0, 3);
    public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");
    public static final BooleanProperty CRAFTING = BooleanProperty.create("crafting");
    public static final BasicEnumProperty TRIAL_SPAWNER_STATE = BasicEnumProperty.create("trial_spawner_state", "inactive", "waiting_for_players", "active", "waiting_for_reward_ejection", "ejecting_reward", "cooldown");
    public static final BasicEnumProperty VAULT_STATE = BasicEnumProperty.create("vault_state", "inactive", "active", "unlocking", "ejecting");
    public static final BooleanProperty OMINOUS = BooleanProperty.create("ominous");
}
