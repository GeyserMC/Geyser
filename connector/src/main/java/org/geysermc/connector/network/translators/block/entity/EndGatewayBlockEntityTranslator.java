package org.geysermc.connector.network.translators.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.LongTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.IntTag;
import com.nukkitx.nbt.tag.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class EndGatewayBlockEntityTranslator extends BlockEntityTranslator {
    @Override
    public List<Tag<?>> translateTag(CompoundTag tag) {
        List<Tag<?>> tags = new ArrayList<>();
        tags.add(new IntTag("Age", (int) (long) tag.get("Age").getValue()));
        // Java sometimes does not provide this tag, but Bedrock crashes if it doesn't exist
        // Linked coordinates
        List<IntTag> tagsList = new ArrayList<>();
        tagsList.add(new IntTag("", getExitPortalCoordinate(tag, "X")));
        tagsList.add(new IntTag("", getExitPortalCoordinate(tag, "Y")));
        tagsList.add(new IntTag("", getExitPortalCoordinate(tag, "Z")));
        com.nukkitx.nbt.tag.ListTag<IntTag> exitPortal =
                new com.nukkitx.nbt.tag.ListTag<>("ExitPortal", com.nukkitx.nbt.tag.IntTag.class, tagsList);
        tags.add(exitPortal);
        return tags;
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = getConstantJavaTag(javaId, x, y, z);
        tag.put(new LongTag("Age"));
        return tag;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        List<IntTag> tagsList = new ArrayList<>();
        tagsList.add(new IntTag("", 0));
        tagsList.add(new IntTag("", 0));
        tagsList.add(new IntTag("", 0));
        tagBuilder.listTag("ExitPortal", IntTag.class, tagsList);
        return tagBuilder.buildRootTag();
    }

    private int getExitPortalCoordinate(CompoundTag tag, String axis) {
        // Return 0 if it doesn't exist, otherwise give proper value
        if (tag.get("ExitPortal") != null) {
            LinkedHashMap compoundTag = (LinkedHashMap) tag.get("ExitPortal").getValue();
            com.github.steveice10.opennbt.tag.builtin.IntTag intTag = (com.github.steveice10.opennbt.tag.builtin.IntTag) compoundTag.get(axis);
            return intTag.getValue();
        } return 0;
    }
}
