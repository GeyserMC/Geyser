/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.BlockEntityUtils;
import org.geysermc.connector.utils.FileUtils;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;

/**
 * The class that all block entities (on both Java and Bedrock) should translate with
 */
public abstract class BlockEntityTranslator {
    public static final Map<String, BlockEntityTranslator> BLOCK_ENTITY_TRANSLATORS = new HashMap<>();
    /**
     * A list of all block entities that only exist on Bedrock
     */
    public static final ObjectArrayList<BedrockOnlyBlockEntity> BEDROCK_ONLY_BLOCK_ENTITIES = new ObjectArrayList<>();

    /**
     * Contains a list of irregular block entity name translations that can't be fit into the regex
     */
    public static final Map<String, String> BLOCK_ENTITY_TRANSLATIONS = new HashMap<String, String>() {
        {
            // Bedrock/Java differences
            put("minecraft:enchanting_table", "EnchantTable");
            put("minecraft:jigsaw", "JigsawBlock");
            put("minecraft:piston_head", "PistonArm");
            put("minecraft:trapped_chest", "Chest");
            // There are some legacy IDs sent but as far as I can tell they are not needed for things to work properly
        }
    };

    protected BlockEntityTranslator() {
    }

    public static void init() {
        // no-op
    }

    static {
        Reflections ref = GeyserConnector.getInstance().useXmlReflections() ? FileUtils.getReflections("org.geysermc.connector.network.translators.world.block.entity") : new Reflections("org.geysermc.connector.network.translators.world.block.entity");
        for (Class<?> clazz : ref.getTypesAnnotatedWith(BlockEntity.class)) {
            GeyserConnector.getInstance().getLogger().debug("Found annotated block entity: " + clazz.getCanonicalName());

            try {
                BLOCK_ENTITY_TRANSLATORS.put(clazz.getAnnotation(BlockEntity.class).name(), (BlockEntityTranslator) clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error("Could not instantiate annotated block entity" + clazz.getCanonicalName());
            }
        }
        for (Class<?> clazz : ref.getSubTypesOf(BedrockOnlyBlockEntity.class)) {
            GeyserConnector.getInstance().getLogger().debug("Found Bedrock-only block entity: " + clazz.getCanonicalName());

            try {
                BedrockOnlyBlockEntity bedrockOnlyBlockEntity = (BedrockOnlyBlockEntity) clazz.newInstance();
                BEDROCK_ONLY_BLOCK_ENTITIES.add(bedrockOnlyBlockEntity);
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error("Could not instantiate annotated block state " + clazz.getCanonicalName());
            }
        }
    }

    public abstract void translateTag(NbtMapBuilder builder, CompoundTag tag, int blockState);

    public NbtMap getBlockEntityTag(String id, CompoundTag tag, int blockState) {
        int x = ((IntTag) tag.getValue().get("x")).getValue();
        int y = ((IntTag) tag.getValue().get("y")).getValue();
        int z = ((IntTag) tag.getValue().get("z")).getValue();

        NbtMapBuilder tagBuilder = getConstantBedrockTag(BlockEntityUtils.getBedrockBlockEntityId(id), x, y, z);
        translateTag(tagBuilder, tag, blockState);
        return tagBuilder.build();
    }

    protected CompoundTag getConstantJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = new CompoundTag("");
        tag.put(new IntTag("x", x));
        tag.put(new IntTag("y", y));
        tag.put(new IntTag("z", z));
        tag.put(new StringTag("id", javaId));
        return tag;
    }

    protected NbtMapBuilder getConstantBedrockTag(String bedrockId, int x, int y, int z) {
        return NbtMap.builder()
                .putInt("x", x)
                .putInt("y", y)
                .putInt("z", z)
                .putString("id", bedrockId);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getOrDefault(Tag tag, T defaultValue) {
        return (tag != null && tag.getValue() != null) ? (T) tag.getValue() : defaultValue;
    }
}
