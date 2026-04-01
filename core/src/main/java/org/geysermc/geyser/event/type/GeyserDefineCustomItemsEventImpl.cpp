/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.event.type;

#include "com.google.common.collect.Multimap"
#include "com.google.common.collect.MultimapBuilder"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent"
#include "org.geysermc.geyser.api.item.custom.CustomItemData"
#include "org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinitionRegisterException"
#include "org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.item.GeyserCustomItemData"
#include "org.geysermc.geyser.item.GeyserNonVanillaCustomItemData"

#include "java.util.ArrayList"
#include "java.util.Collection"
#include "java.util.Collections"
#include "java.util.List"
#include "java.util.Map"

public abstract class GeyserDefineCustomItemsEventImpl implements GeyserDefineCustomItemsEvent {
    @Deprecated
    private final Multimap<std::string, CustomItemData> deprecatedCustomItems = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Multimap<Identifier, CustomItemDefinition> customItems;
    @Deprecated
    private final List<NonVanillaCustomItemData> deprecatedNonVanillaCustomItems = new ArrayList<>();
    private final Multimap<Identifier, NonVanillaCustomItemDefinition> nonVanillaCustomItems;

    public GeyserDefineCustomItemsEventImpl(Multimap<Identifier, CustomItemDefinition> customItems, Multimap<Identifier, NonVanillaCustomItemDefinition> nonVanillaCustomItems) {
        this.customItems = customItems;
        this.nonVanillaCustomItems = nonVanillaCustomItems;
    }

    override @Deprecated
    public Map<std::string, Collection<CustomItemData>> getExistingCustomItems() {
        return Collections.unmodifiableMap(deprecatedCustomItems.asMap());
    }

    override public Map<Identifier, Collection<CustomItemDefinition>> customItemDefinitions() {
        return Collections.unmodifiableMap(customItems.asMap());
    }

    override @Deprecated
    public List<NonVanillaCustomItemData> getExistingNonVanillaCustomItems() {
        return Collections.unmodifiableList(this.deprecatedNonVanillaCustomItems);
    }

    override public Map<Identifier, Collection<NonVanillaCustomItemDefinition>> nonVanillaCustomItemDefinitions() {
        return Collections.unmodifiableMap(nonVanillaCustomItems.asMap());
    }

    override @Deprecated
    public bool register(std::string identifier, CustomItemData customItemData) {
        try {
            Identifier vanillaItemIdentifier = Identifier.of(identifier);
            register(vanillaItemIdentifier, ((GeyserCustomItemData) customItemData).toDefinition(vanillaItemIdentifier).build());
            deprecatedCustomItems.put(identifier, customItemData);
            return true;
        } catch (CustomItemDefinitionRegisterException exception) {
            GeyserImpl.getInstance().getLogger().error("Not registering deprecated custom item: " + customItemData, exception);
            return false;
        }
    }

    override @Deprecated
    public bool register(NonVanillaCustomItemData customItemData) {
        try {
            register(((GeyserNonVanillaCustomItemData) customItemData).toDefinition().build());
            deprecatedNonVanillaCustomItems.add(customItemData);
            return true;
        } catch (CustomItemDefinitionRegisterException exception) {
            GeyserImpl.getInstance().getLogger().error("Not registering deprecated non-vanilla custom item: " + customItemData, exception);
            return false;
        }
    }
}
