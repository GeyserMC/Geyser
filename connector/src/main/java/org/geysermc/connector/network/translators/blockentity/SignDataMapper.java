package org.geysermc.connector.network.translators.blockentity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import org.geysermc.connector.network.translators.BlockEntityUtils;
import org.geysermc.connector.utils.MessageUtils;

public class SignDataMapper extends BlockEntityUtils.ExtraDataMapper {
    @Override
    public com.nukkitx.nbt.tag.CompoundTag getExtraTags(CompoundTag tag) {
        String text = c(tag.get("Text1").getValue().toString()) + "\n" +
                c(tag.get("Text2").getValue().toString()) + "\n" +
                c(tag.get("Text3").getValue().toString()) + "\n" +
                c(tag.get("Text4").getValue().toString());

        int x = ((Number) tag.getValue().get("x").getValue()).intValue();
        int y = ((Number) tag.getValue().get("y").getValue()).intValue();
        int z = ((Number) tag.getValue().get("z").getValue()).intValue();

        String id = BlockEntityUtils.getBedrockID((String) tag.get("id").getValue());

        System.out.println(text);

        return CompoundTagBuilder.builder()
                .stringTag("id", id)
                .stringTag("Text", text)
                .intTag("x", x)
                .intTag("y", y)
                .intTag("z", z)
                .byteTag("isMovable", (byte) 0)
                .build("");
    }

    //One letter name because I rly don't want to make the code at the top more than whats already there.
    private String c(String string) {
        return MessageUtils.getBedrockMessage(string);
    }
}
