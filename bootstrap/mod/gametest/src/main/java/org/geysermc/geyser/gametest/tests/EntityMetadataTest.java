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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.gametest.mixin.SynchedEntityDataAccessor;
import org.geysermc.geyser.gametest.util.SynchedEntityDataDebugger;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.translator.entity.EntityMetadataTranslator;

public class EntityMetadataTest extends GeyserTestInstance {
    public static final MapCodec<EntityMetadataTest> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        commonFields(instance)
            .and(EntityType.CODEC.fieldOf("entity_type").forGetter(test -> test.entityType))
            .apply(instance, EntityMetadataTest::new)
    );
    private final EntityType<?> entityType;

    private EntityMetadataTest(RegistryOps<?> ops, boolean required, EntityType<?> entityType) {
        super(ops, required);
        this.entityType = entityType;
    }

    public EntityMetadataTest(HolderLookup.Provider registries, boolean required, EntityType<?> entityType) {
        super(registries, required);
        this.entityType = entityType;
    }

    @Override
    public void run(GameTestHelper helper) {
        // Nice full qualified name, lovely
        org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType geyserEntityType = org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath().toUpperCase());
        EntityDefinition<?> definition = Registries.ENTITY_DEFINITIONS.get(geyserEntityType);

        if (definition == null) {
            helper.fail("No entity definition found for type " + entityType);
        } else {
            Entity javaEntity;
            if (entityType == EntityType.PLAYER) {
                javaEntity = helper.makeMockPlayer(GameType.SURVIVAL);
            } else {
                javaEntity = entityType.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            }

            SynchedEntityData synchedEntityData = javaEntity.getEntityData();
            SynchedEntityData.DataItem<?>[] dataItems = ((SynchedEntityDataAccessor) synchedEntityData).getItemsById();

            try {
                helper.assertValueEqual(definition.translators().size(), dataItems.length, "metadata translators for entity type " + entityType);

                for (int i = 0; i < dataItems.length; i++) {
                    EntityMetadataTranslator<?, ?, ?> translator = definition.translators().get(i);
                    if (translator == null) {
                        // TODO warn for this ? somehow?
                        continue;
                    }

                    int expectedId = EntityDataSerializers.getSerializedId(dataItems[i].getAccessor().serializer());
                    int geyserId = translator.acceptedType().getId();
                    helper.assertValueEqual(geyserId, expectedId, "serializer for " + entityType + " at metadata index " + i + " ("
                        + SynchedEntityDataDebugger.findNameOfSerializer(dataItems[i].getAccessor().serializer()) + ")");
                }
            } catch (GameTestAssertException exception) {
                GeyserImpl.getInstance().getLogger().warning("Metadata for entity type " + entityType + " are as follows:\n" + SynchedEntityDataDebugger.prettyPrintEntityDataAccessors(javaEntity.getClass(), dataItems));
                throw exception;
            } finally {
                javaEntity.discard();
            }
        }

        helper.succeed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return MAP_CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Geyser Entity Metadata Test for " + entityType);
    }
}
