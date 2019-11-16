package org.geysermc.connector.utils;

import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.Tag;

public class BlockEntityUtils {
    public static final String CHEST = "Chest";
    public static final String ENDER_CHEST = "EnderChest";
    public static final String FURNACE = "Furnace";
    public static final String SIGN = "Sign";
    public static final String MOB_SPAWNER = "MobSpawner";
    public static final String ENCHANT_TABLE = "EnchantTable";
    public static final String SKULL = "Skull";
    public static final String FLOWER_POT = "FlowerPot";
    public static final String BREWING_STAND = "BrewingStand";
    public static final String DAYLIGHT_DETECTOR = "DaylightDetector";
    public static final String MUSIC = "Music";
    public static final String CAULDRON = "Cauldron";
    public static final String BEACON = "Beacon";
    public static final String PISTON_ARM = "PistonArm";
    public static final String COMPARATOR = "Comparator";
    public static final String HOPPER = "Hopper";
    public static final String BED = "Bed";
    public static final String JUKEBOX = "Jukebox";
    public static final String SHULKER_BOX = "ShulkerBox";
    public static final String BANNER = "Banner";

    public static final String MINECRAFT = "minecraft:";

    public static String getBedrockID(String java) {
        java = java.replace(MINECRAFT, "");

        if(java.equalsIgnoreCase("chest"))
            return CHEST;
        if(java.equalsIgnoreCase("ender_chest"))
            return ENDER_CHEST;
        if(java.equalsIgnoreCase("furnace"))
            return FURNACE;
        //Signs are special
        if(java.contains("sign"))
            return SIGN;
        if(java.equalsIgnoreCase("mob_spawner"))
            return MOB_SPAWNER;
        if(java.equalsIgnoreCase("enchanting_table"))
            return ENCHANT_TABLE;
        if(java.equalsIgnoreCase("skull"))
            return SKULL;
        if(java.equalsIgnoreCase("flower_pot"))
            return FLOWER_POT;
        if(java.equalsIgnoreCase("brewing_stand"))
            return BREWING_STAND;
        if(java.equalsIgnoreCase("daylight_detector"))
            return DAYLIGHT_DETECTOR;
        if(java.equalsIgnoreCase("note_block"))
            return MUSIC;
        if(java.equalsIgnoreCase("cauldron"))
            return CAULDRON;
        if(java.equalsIgnoreCase("beacon"))
            return BEACON;
        if(java.equalsIgnoreCase("piston_head"))
            return PISTON_ARM;
        if(java.equalsIgnoreCase("comparator"))
            return COMPARATOR;
        if(java.equalsIgnoreCase("hopper"))
            return HOPPER;
        if(java.equalsIgnoreCase("bed"))
            return BED;
        if(java.equalsIgnoreCase("jukebox"))
            return JUKEBOX;
        if(java.equalsIgnoreCase("shulker_box"))
            return SHULKER_BOX;
        if(java.equalsIgnoreCase("banner"))
            return BANNER;

        return null;
    }

    //public static Tag<?> getExtraData(CompoundTag tag)
}
