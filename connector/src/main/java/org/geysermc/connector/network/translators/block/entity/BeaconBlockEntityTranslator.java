package org.geysermc.connector.network.translators.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BeaconBlockEntityTranslator extends BlockEntityTranslator {


    @Override
    public List<Tag<?>> translateTag(com.github.steveice10.opennbt.tag.builtin.CompoundTag tag) {
        List<Tag<?>> tags = new ArrayList<>();
        int primary = getOrDefault(tag.getValue().get("Primary"), 0);
        int secondary = getOrDefault(tag.getValue().get("Secondary"), 0);
        int levels = getOrDefault(tag.getValue().get("Levels"), 0);
        System.out.println("Primary: " + primary);
        System.out.println("Secondary: " + secondary);
        System.out.println("Levels: " + levels);
        String lock = getOrDefault(tag.getValue().get("Lock"), "");
        tags.add(new com.nukkitx.nbt.tag.IntTag("Primary", primary));
        tags.add(new com.nukkitx.nbt.tag.IntTag("Secondary", secondary));
        tags.add(new com.nukkitx.nbt.tag.IntTag("Levels", levels));
        tags.add(new com.nukkitx.nbt.tag.StringTag("Lock", lock));

        return tags;
    }

    @Override
    public com.github.steveice10.opennbt.tag.builtin.CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = getConstantJavaTag(javaId, x, y, z);
        tag.put(new IntTag("Primary"));
        tag.put(new IntTag("Secondary"));
        tag.put(new IntTag("Levels"));
        tag.put(new StringTag("Lock"));
        return tag;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        tagBuilder.intTag("Secondary", 0);
        tagBuilder.intTag("Primary", 0);
//        tagBuilder.intTag("Levels", 0);
//        // Apparently locks the beacon.
//        tagBuilder.stringTag("Lock", "");
        return tagBuilder.buildRootTag();
    }


}
