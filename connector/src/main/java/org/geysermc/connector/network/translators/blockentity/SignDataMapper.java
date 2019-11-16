package org.geysermc.connector.network.translators.blockentity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.tag.IntTag;
import com.nukkitx.nbt.tag.StringTag;
import com.nukkitx.nbt.tag.Tag;
import org.geysermc.connector.network.translators.BlockEntityUtils;

import java.util.ArrayList;
import java.util.List;

public class SignDataMapper extends BlockEntityUtils.ExtraDataMapper {
    @Override
    public List<Tag<?>> getExtraTags(CompoundTag tag) {
        List<Tag<?>> list = new ArrayList<>();

        list.add(new StringTag("Text", tag.get("Text1").getValue().toString() + "\n" +
                tag.get("Text2").getValue().toString() + "\n" +
                tag.get("Text3").getValue().toString() + "\n" +
                tag.get("Text4").getValue().toString()));
        int x = ((Number) tag.getValue().get("x").getValue()).intValue();
        int y = ((Number) tag.getValue().get("y").getValue()).intValue();
        int z = ((Number) tag.getValue().get("z").getValue()).intValue();

        String id = BlockEntityUtils.getBedrockID((String) tag.get("id").getValue());

        list.add(new IntTag("x", x));
        list.add(new IntTag("y", y));
        list.add(new IntTag("z", z));

        list.add(new StringTag("id", id));

        return list;
    }
}
