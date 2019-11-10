package org.geysermc.connector.network.translators.effect;

import com.github.steveice10.mc.protocol.data.game.world.sound.BuiltinSound;
import org.geysermc.connector.network.translators.TranslatorsInit;

public class SoundTranslator {
    private static final String[] BLOCK_OPS = {"BREAK", "HIT", "USE", "STEP", "FALL"};

    public static void translate() {
        for(BuiltinSound jSound : BuiltinSound.values()) {
            try {
                for(Sound bSound : Sound.values()) {
                    if(converted(jSound.name(), true).equalsIgnoreCase(converted(bSound.name(), false))) {
                        TranslatorsInit.SOUNDS.put(jSound, bSound);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String converted(String string, boolean java) {
        if(java && string.contains("BLOCK")) {
            for (String blockOp : BLOCK_OPS) {
                if (string.endsWith(blockOp)) {
                    string = blockOp + string.replace(blockOp, "");
                    System.out.println(string);
                }
            }
        }

        string = string.replaceAll("_", "")
                .replaceAll("RANDOM", "")
                .replaceAll("MUSICDISC", "RECORD")
                .replaceAll("BLOCK", "")
                .replaceAll("ENTITY", "")
                .replaceAll("EVENT", "")
                .replaceAll("GAME", "")
                .replaceAll("EVOCATIONILLAGER", "EVOKER")
                .replaceAll("MOB", "")
                .replaceAll("BREAK", "DIG")
                .replaceAll("WOOL", "CLOTH")
                .replaceAll("FALL", "LAND")
                .replaceAll("IDLE", "AMBIENT")
                .replaceAll("HURT", "HIT")
                .replaceAll("ENDERMEN", "ENDERMAN")
                .replaceAll("KILL", "DEATH")
                .replaceAll("ITEM", "")
                .replaceAll("HORSEDONKEY", "DONKEY")
                .replaceAll("HORSESKELETON", "SKELETONHORSE")
                .replaceAll("HORSEZOMBIE", "ZOMBIEHORSE");

        return string;
    }
}
