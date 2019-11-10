package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.v361.BedrockUtils;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.translators.block.BlockEntry;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.world.GlobalBlockPalette;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.*;

public class Toolbox {

    public static final Collection<StartGamePacket.ItemEntry> ITEMS;
    public static final ByteBuf CACHED_PALLETE;
    public static final ItemData[] CREATIVE_ITEMS;

    public static final TIntObjectMap<ItemEntry> ITEM_ENTRIES;
    public static final TIntObjectMap<BlockEntry> BLOCK_ENTRIES;

    static {
        InputStream stream = Toolbox.class.getClassLoader().getResourceAsStream("bedrock/cached_palette.json");
        ObjectMapper mapper = new ObjectMapper();
        List<LinkedHashMap<String, Object>> entries = new ArrayList<>();

        try {
            entries = mapper.readValue(stream, ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteBuf cachedPalette = Unpooled.buffer();
        VarInts.writeUnsignedInt(cachedPalette, entries.size());

        Map<String, Integer> blockIdToIdentifier = new HashMap<>();

        for (Map<String, Object> entry : entries) {
            blockIdToIdentifier.put((String) entry.get("name"), (int) entry.get("id"));

            GlobalBlockPalette.registerMapping((int) entry.get("id") << 4 | (int) entry.get("data"));
            BedrockUtils.writeString(cachedPalette, (String) entry.get("name"));
            cachedPalette.writeShortLE((int) entry.get("data"));
            cachedPalette.writeShortLE((int) entry.get("id"));
        }

        CACHED_PALLETE = cachedPalette;

        InputStream stream2 = Toolbox.class.getClassLoader().getResourceAsStream("bedrock/items.json");
        if (stream2 == null) {
            throw new AssertionError("Items Table not found");
        }

        ObjectMapper startGameItemMapper = new ObjectMapper();
        List<Map> startGameItems = new ArrayList<>();
        try {
            startGameItems = startGameItemMapper.readValue(stream2, ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<StartGamePacket.ItemEntry> startGameEntries = new ArrayList<>();
        for (Map entry : startGameItems) {
            startGameEntries.add(new StartGamePacket.ItemEntry((String) entry.get("name"), (short) ((int) entry.get("id"))));
        }

        ITEMS = startGameEntries;

        InputStream itemStream = Toolbox.class.getClassLoader().getResourceAsStream("items.json");
        ObjectMapper itemMapper = new ObjectMapper();
        Map<String, Map<String, Object>> items = new HashMap<>();

        try {
            items = itemMapper.readValue(itemStream, LinkedHashMap.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        TIntObjectMap<ItemEntry> itemEntries = new TIntObjectHashMap<>();
        int itemIndex = 0;

        for (Map.Entry<String, Map<String, Object>> itemEntry : items.entrySet()) {
            itemEntries.put(itemIndex, new ItemEntry(itemEntry.getKey(), itemIndex, (int) itemEntry.getValue().get("bedrock_id"), (int) itemEntry.getValue().get("bedrock_data")));
            itemIndex++;
        }

        ITEM_ENTRIES = itemEntries;

        InputStream blockStream = Toolbox.class.getClassLoader().getResourceAsStream("blocks.json");
        ObjectMapper blockMapper = new ObjectMapper();
        Map<String, Map<String, Object>> blocks = new HashMap<>();

        try {
            blocks = blockMapper.readValue(blockStream, LinkedHashMap.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        TIntObjectMap<BlockEntry> blockEntries = new TIntObjectHashMap<>();
        int blockIndex = 0;

        for (Map.Entry<String, Map<String, Object>> itemEntry : blocks.entrySet()) {
            if (!blockIdToIdentifier.containsKey(itemEntry.getValue().get("bedrock_identifier"))) {
                GeyserLogger.DEFAULT.debug("Mapping " + itemEntry.getValue().get("bedrock_identifier") + " does not exist on bedrock edition!");
                blockEntries.put(blockIndex, new BlockEntry(itemEntry.getKey(), blockIndex, 248, 0)); // update block
            } else {
                blockEntries.put(blockIndex, new BlockEntry(itemEntry.getKey(), blockIndex, blockIdToIdentifier.get(itemEntry.getValue().get("bedrock_identifier")), (int) itemEntry.getValue().get("bedrock_data")));
            }

            blockIndex++;
        }

        BLOCK_ENTRIES = blockEntries;

        InputStream creativeItemStream = Toolbox.class.getClassLoader().getResourceAsStream("bedrock/creative_items.json");
        ObjectMapper creativeItemMapper = new ObjectMapper();
        List<LinkedHashMap<String, Object>> creativeItemEntries = new ArrayList<>();

        try {
            creativeItemEntries = creativeItemMapper.readValue(creativeItemStream, ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<ItemData> creativeItems = new ArrayList<>();
        for (Map<String, Object> map : creativeItemEntries) {
            short damage = 0;
            if (map.containsKey("damage")) {
                damage = (short)(int) map.get("damage");
            }
            if (map.containsKey("nbt_b64")) {
                byte[] bytes = DatatypeConverter.parseBase64Binary((String) map.get("nbt_b64"));
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                try {
                    com.nukkitx.nbt.tag.CompoundTag tag = (com.nukkitx.nbt.tag.CompoundTag) NbtUtils.createReaderLE(bais).readTag();
                    creativeItems.add(ItemData.of((int) map.get("id"), damage, 1, tag));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                creativeItems.add(ItemData.of((int) map.get("id"), damage, 1));
            }
        }

        CREATIVE_ITEMS = creativeItems.toArray(new ItemData[0]);
    }
}