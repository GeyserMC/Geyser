/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.gametest.tests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.gametest.GameTestUtil;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.concurrent.atomic.AtomicInteger;

public class EntityMetadataTest extends GameTestInstance {
    private static final Int2ObjectMap<MetadataType<?>> ID_MAP_MCPL = new Int2ObjectOpenHashMap<>();

    static {
        // Load all known entity metadata
        int size = MetadataTypes.size();
        for (int i = 0; i < size; i++) {
            MetadataType<?> type = MetadataTypes.from(i);
            ID_MAP_MCPL.put(i, type);
        }
    }

    public static final MapCodec<EntityMetadataTest> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                GameTestUtil.REGISTRY_OPS_MAP_CODEC.forGetter(ignored -> null),
                Codec.BOOL.optionalFieldOf("required", true).forGetter(GameTestInstance::required)
            ).apply(instance, EntityMetadataTest::new)
    );

    public EntityMetadataTest(RegistryOps<?> ops, boolean required) {
        super(GameTestUtil.createEmptyTestData(ops, required));
    }

    @Override
    public void run(GameTestHelper helper) {
        AtomicInteger errors = new AtomicInteger();

        BuiltInRegistries.ENTITY_TYPE.stream().forEach(entityType -> {
            Entity javaEntity = entityType.create(helper.getLevel().getLevel(), EntitySpawnReason.COMMAND);
            if (javaEntity == null) {
                return;
            }

            try {
                EntityType geyserEntityType = EntityType.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath().toUpperCase());
                EntityDefinition<?> definition = Registries.ENTITY_DEFINITIONS.get(geyserEntityType);

                if (definition == null) {
                    GeyserImpl.getInstance().getLogger().warning("No definition found for entity type " + geyserEntityType);
                    return;
                }

                SynchedEntityData synchedEntityData = javaEntity.getEntityData();
                int translators = synchedEntityData.itemsById.length;

                if (definition.translators().size() != translators) {
                    GeyserImpl.getInstance().getLogger().warning("Expected " + definition.translators().size() + " translators, found " + translators + " for " + geyserEntityType);
                }

            } finally {
                javaEntity.discard();
            }
        });

        if (errors.get() > 0) {
            helper.fail("Failed to validate " + errors.get() + " entity types");
        } else {
            helper.succeed();
        }
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return MAP_CODEC;
    }

    @Override
    protected net.minecraft.network.chat.MutableComponent typeDescription() {
        return net.minecraft.network.chat.Component.literal("Geyser Entity Metadata Test");
    }
}
