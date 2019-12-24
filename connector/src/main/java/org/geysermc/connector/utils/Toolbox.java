package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTInputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.ListTag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.nbt.tag.Tag;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.translators.block.BlockEntry;
import org.geysermc.connector.network.translators.item.ItemEntry;

import java.io.*;
import java.util.*;

public class Toolbox {

    public static final Collection<StartGamePacket.ItemEntry> ITEMS = new ArrayList<>();
    public static ListTag<CompoundTag> BLOCKS;
    public static ItemData[] CREATIVE_ITEMS;

    public static final Int2ObjectMap<ItemEntry> ITEM_ENTRIES = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<BlockEntry> BLOCK_ENTRIES = new Int2ObjectOpenHashMap<>();

    public static void init() {
        InputStream stream = GeyserConnector.class.getClassLoader().getResourceAsStream("bedrock/runtime_block_states.dat");
        if (stream == null) {
            throw new AssertionError("Unable to find bedrock/runtime_block_states.dat");
        }

        ListTag<CompoundTag> blocksTag;

        NBTInputStream nbtInputStream = NbtUtils.createNetworkReader(stream);
        try {
            blocksTag = (ListTag<CompoundTag>) nbtInputStream.readTag();
            nbtInputStream.close();
        } catch (Exception ex) {
            GeyserLogger.DEFAULT.warning("Failed to get blocks from runtime block states, please report this error!");
            throw new AssertionError(ex);
        }

        BLOCKS = blocksTag;
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

        for (Map entry : startGameItems) {
            ITEMS.add(new StartGamePacket.ItemEntry((String) entry.get("name"), (short) ((int) entry.get("id"))));
        }

        InputStream itemStream = Toolbox.class.getClassLoader().getResourceAsStream("mappings/items.json");
        ObjectMapper itemMapper = new ObjectMapper();
        Map<String, Map<String, Object>> items = new HashMap<>();

        try {
            items = itemMapper.readValue(itemStream, LinkedHashMap.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int itemIndex = 0;
        for (Map.Entry<String, Map<String, Object>> itemEntry : items.entrySet()) {
            ITEM_ENTRIES.put(itemIndex, new ItemEntry(itemEntry.getKey(), itemIndex, (int) itemEntry.getValue().get("bedrock_id"), (int) itemEntry.getValue().get("bedrock_data")));
            itemIndex++;
        }

        InputStream blockStream = Toolbox.class.getClassLoader().getResourceAsStream("mappings/blocks.json");
        ObjectMapper blockMapper = new ObjectMapper();
        Map<String, Map<String, Object>> blocks = new HashMap<>();

        try {
            blocks = blockMapper.readValue(blockStream, LinkedHashMap.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int javaIndex = -1;
        javaLoop:
        for (Map.Entry<String, Map<String, Object>> javaEntry : blocks.entrySet()) {
            javaIndex++;
            String wantedIdentifier = (String) javaEntry.getValue().get("bedrock_identifier");
            Map<String, Object> wantedStates = (Map<String, Object>) javaEntry.getValue().get("bedrock_states");

            int bedrockIndex = -1;
            bedrockLoop:
            for (CompoundTag bedrockEntry : BLOCKS.getValue()) {
                bedrockIndex++;
                CompoundTag blockTag = bedrockEntry.getAsCompound("block");
                if (blockTag.getAsString("name").equals(wantedIdentifier)) {
                    if (wantedStates != null) {
                        Map<String, Tag<?>> bedrockStates = blockTag.getAsCompound("states").getValue();
                        for (Map.Entry<String, Object> stateEntry : wantedStates.entrySet()) {
                            Tag<?> bedrockStateTag = bedrockStates.get(stateEntry.getKey());
                            if (bedrockStateTag == null)
                                continue bedrockLoop;
                            Object bedrockStateValue = bedrockStateTag.getValue();
                            if (bedrockStateValue instanceof Byte)
                                bedrockStateValue = ((Byte) bedrockStateValue).intValue();
                            if (!stateEntry.getValue().equals(bedrockStateValue))
                                continue bedrockLoop;
                        }
                    }
                    BlockEntry blockEntry = new BlockEntry(javaEntry.getKey(), javaIndex, bedrockIndex);
                    BLOCK_ENTRIES.put(javaIndex, blockEntry);
                    continue javaLoop;
                }
            }
            GeyserLogger.DEFAULT.debug("Mapping " + javaEntry.getKey() + " was not found for bedrock edition!");
        }

        InputStream creativeItemStream = Toolbox.class.getClassLoader().getResourceAsStream("bedrock/creative_items.json");
        ObjectMapper creativeItemMapper = new ObjectMapper();
        List<LinkedHashMap<String, Object>> creativeItemEntries = new ArrayList<>();

        try {
            creativeItemEntries = (ArrayList<LinkedHashMap<String, Object>>) creativeItemMapper.readValue(creativeItemStream, HashMap.class).get("items");
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
                byte[] bytes = Base64.getDecoder().decode((String) map.get("nbt_b64"));
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