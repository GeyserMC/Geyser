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

package org.geysermc.geyser.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.references.BlockIds;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Dialogs;
import net.minecraft.server.dialog.ServerLinksDialog;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DialogTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.geysermc.geyser.gametest.tests.EntityMetadataTest;
import org.geysermc.geyser.gametest.tests.GeyserHolderSetTestInstance;
import org.geysermc.geyser.gametest.tests.JavaPacketTranslatorExistenceTest;
import org.geysermc.geyser.gametest.tests.ResolvableComponentLoadingTestInstance;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final  class GeyserGameTests {
    private static final List<EntityType<?>> UNSUPPORTED_ENTITY_TYPES = List.of(EntityTypes.BLOCK_DISPLAY, EntityTypes.ITEM_DISPLAY, EntityTypes.MARKER);

    private GeyserGameTests() {}

    private static ResourceKey<GameTestInstance> createKey(String name) {
        return ResourceKey.create(Registries.TEST_INSTANCE, Identifier.fromNamespaceAndPath("geyser", name));
    }

    private static ResourceKey<GameTestInstance> createKey(Identifier testType, String name) {
        return createKey(testType.getPath() + "/" + name);
    }

    private static ResourceKey<GameTestInstance> createKey(Identifier testType, Identifier testInstance) {
        return createKey(testType, testInstance.getPath());
    }

    private static ResourceKey<GameTestInstance> createSingletonKey(Identifier testType) {
        return createKey(testType.getPath());
    }

    private static void registerSingletonTest(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, FabricDynamicRegistryProvider.Entries entries, GeyserGameTestTypes.SingletonTestType type, boolean required) {
        entries.add(createSingletonKey(type.type()), type.constructor().create(testEnvironments, required));
    }

    private static void registerSingletonTest(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, FabricDynamicRegistryProvider.Entries entries, GeyserGameTestTypes.SingletonTestType type) {
        registerSingletonTest(testEnvironments, entries, type, true);
    }

    private static void registerEntityTypeTests(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, FabricDynamicRegistryProvider.Entries entries) {
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            entries.add(createKey(GeyserGameTestTypes.ENTITY_METADATA, BuiltInRegistries.ENTITY_TYPE.getKey(entityType)),
                new EntityMetadataTest(testEnvironments, !UNSUPPORTED_ENTITY_TYPES.contains(entityType), entityType));
        }
    }

    private static void registerResolvableComponentLoadingTests(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, GeyserGameTestPlatform platform, FabricDynamicRegistryProvider.Entries entries) {
        for (Holder<Item> item : findItemsWithResolvableComponents(platform)) {
            entries.add(createKey(GeyserGameTestTypes.RESOLVABLE_COMPONENTS, item.unwrapKey().orElseThrow().identifier()),
                new ResolvableComponentLoadingTestInstance(testEnvironments, true, item));
        }
    }

    private static void registerPacketTranslatorTests(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, FabricDynamicRegistryProvider.Entries entries, ProtocolInfo.DetailsProvider protocol) {
        JavaPacketTranslatorExistenceTest.createForProtocol(testEnvironments, true, protocol)
            .forEach(test -> entries.add(createKey(GeyserGameTestTypes.PACKET_TRANSLATOR_EXISTENCE, test.packetId()), test));
    }

    private static <T> void registerHolderSetTest(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, FabricDynamicRegistryProvider.Entries entries,
                                                  ResourceKey<? extends Registry<T>> registry, HolderSet<T> holderSet, String name) {
        entries.add(createKey(GeyserGameTestTypes.HOLDER_SET, registry.identifier().getPath() + "/" + name),
            new GeyserHolderSetTestInstance(testEnvironments, true, registry, holderSet));
    }

    private static void registerHolderSetTests(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, HolderLookup.Provider registries, FabricDynamicRegistryProvider.Entries entries) {
        HolderGetter<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
        HolderGetter<Item> items = registries.lookupOrThrow(Registries.ITEM);
        HolderGetter<Dialog> dialogs = registries.lookupOrThrow(Registries.DIALOG);

        registerHolderSetTest(testEnvironments, entries, Registries.BLOCK, blocks.getOrThrow(BlockTags.ALL_SIGNS), "tag");
        registerHolderSetTest(testEnvironments, entries, Registries.BLOCK, HolderSet.direct(), "empty");
        registerHolderSetTest(testEnvironments, entries, Registries.BLOCK, HolderSet.direct(blocks::getOrThrow, BlockIds.BAMBOO_SAPLING, BlockIds.SOUL_FIRE, BlockIds.ACACIA_WALL_SIGN), "direct");

        registerHolderSetTest(testEnvironments, entries, Registries.ITEM, items.getOrThrow(ItemTags.ANVIL), "tag");
        registerHolderSetTest(testEnvironments, entries, Registries.ITEM, HolderSet.direct(), "empty");
        registerHolderSetTest(testEnvironments, entries, Registries.ITEM, HolderSet.direct(items::getOrThrow, ItemIds.ACACIA_BOAT), "single_direct");

        registerHolderSetTest(testEnvironments, entries, Registries.DIALOG, dialogs.getOrThrow(DialogTags.QUICK_ACTIONS), "tag");
        registerHolderSetTest(testEnvironments, entries, Registries.DIALOG,
            HolderSet.direct(
                dialogs.getOrThrow(Dialogs.CUSTOM_OPTIONS),
                Holder.direct(new ServerLinksDialog(new CommonDialogData(Component.empty(), Optional.empty(), true, false, DialogAction.NONE,
                    List.of(), List.of()), Optional.empty(), 1, 10))
            ),
            "list_with_inline");
        registerHolderSetTest(testEnvironments, entries, Registries.DIALOG, HolderSet.direct(Holder.direct(
            new ServerLinksDialog(new CommonDialogData(Component.empty(), Optional.empty(), true, false, DialogAction.NONE,
                List.of(), List.of()), Optional.empty(), 1, 10)
        )), "single_inline");
    }

    public static void bootstrap(HolderLookup.Provider registries, FabricDynamicRegistryProvider.Entries entries) {
        GeyserGameTestPlatform platform = new GeyserGameTestPlatform();
        HolderGetter<TestEnvironmentDefinition<?>> testEnvironments = registries.lookupOrThrow(Registries.TEST_ENVIRONMENT);

        registerEntityTypeTests(testEnvironments, entries);
        registerSingletonTest(testEnvironments, entries, GeyserGameTestTypes.REQUIRED_COMPONENTS_FOR_HASHING);
        registerSingletonTest(testEnvironments, entries, GeyserGameTestTypes.MINECRAFT_VERSION);
        registerResolvableComponentLoadingTests(testEnvironments, platform, entries);

        registerPacketTranslatorTests(testEnvironments, entries, ConfigurationProtocols.CLIENTBOUND_TEMPLATE);
        registerPacketTranslatorTests(testEnvironments, entries, GameProtocols.CLIENTBOUND_TEMPLATE);

        registerHolderSetTests(testEnvironments, registries, entries);
    }

    private static List<Holder<Item>> findItemsWithResolvableComponents(GeyserGameTestPlatform platform) {
        try (InputStream stream = platform.resolveResource("mappings/resolvable_item_data_components.json")) {
            assert stream != null;
            JsonElement rootElement = JsonParser.parseReader(new InputStreamReader(stream));
            JsonArray items = rootElement.getAsJsonObject().get("value").getAsJsonArray();

            List<Holder<Item>> itemsWithResolvableComponents = new ArrayList<>();

            for (int i = 0; i < items.size(); i++) {
                JsonElement item = items.get(i);
                JsonArray itemComponentArray = item.getAsJsonArray();
                if (!itemComponentArray.isEmpty()) {
                    itemsWithResolvableComponents.add(Objects.requireNonNull(BuiltInRegistries.ITEM.asHolderIdMap().byId(i)));
                }
            }

            return itemsWithResolvableComponents;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
