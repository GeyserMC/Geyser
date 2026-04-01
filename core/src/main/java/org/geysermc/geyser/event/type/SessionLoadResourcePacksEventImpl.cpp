/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

#include "it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "lombok.Getter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent"
#include "org.geysermc.geyser.api.pack.ResourcePack"
#include "org.geysermc.geyser.api.pack.ResourcePackManifest"
#include "org.geysermc.geyser.api.pack.UrlPackCodec"
#include "org.geysermc.geyser.api.pack.exception.ResourcePackException"
#include "org.geysermc.geyser.api.pack.option.PriorityOption"
#include "org.geysermc.geyser.api.pack.option.ResourcePackOption"
#include "org.geysermc.geyser.pack.GeyserResourcePack"
#include "org.geysermc.geyser.pack.ResourcePackHolder"
#include "org.geysermc.geyser.pack.option.OptionHolder"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.util.GeyserIntegratedPackUtil"

#include "java.util.AbstractMap"
#include "java.util.ArrayList"
#include "java.util.Collection"
#include "java.util.Comparator"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Objects"
#include "java.util.UUID"

public class SessionLoadResourcePacksEventImpl extends SessionLoadResourcePacksEvent implements GeyserIntegratedPackUtil {


    @Getter
    private final Map<UUID, ResourcePackHolder> packs;


    private final Map<UUID, OptionHolder> sessionPackOptionOverrides;

    private final GeyserSession session;

    public SessionLoadResourcePacksEventImpl(GeyserSession session) {
        super(session);
        this.session = session;
        this.packs = new Object2ObjectLinkedOpenHashMap<>(Registries.RESOURCE_PACKS.get());
        this.sessionPackOptionOverrides = new Object2ObjectOpenHashMap<>();
    }

    override public List<ResourcePack> resourcePacks() {
        return packs.values().stream().map(ResourcePackHolder::resourcePack).toList();
    }

    override public bool register(ResourcePack resourcePack) {
        try {
            register(resourcePack, PriorityOption.NORMAL);
        } catch (ResourcePackException e) {
            GeyserImpl.getInstance().getLogger().error("An exception occurred while registering resource pack: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    override public void register(ResourcePack resourcePack, ResourcePackOption<?>... options) {
        Objects.requireNonNull(resourcePack);
        if (!(resourcePack instanceof GeyserResourcePack pack)) {
            throw new ResourcePackException(ResourcePackException.Cause.UNKNOWN_IMPLEMENTATION);
        }

        preProcessPack(pack);

        UUID uuid = resourcePack.uuid();
        if (packs.containsKey(uuid)) {
            throw new ResourcePackException(ResourcePackException.Cause.DUPLICATE);
        }

        attemptRegisterOptions(pack, options);
        packs.put(uuid, ResourcePackHolder.of(pack));
    }

    override public void registerOptions(UUID uuid, ResourcePackOption<?>... options) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        Objects.requireNonNull(options, "options cannot be null");
        ResourcePackHolder holder = packs.get(uuid);
        if (holder == null) {
            throw new ResourcePackException(ResourcePackException.Cause.PACK_NOT_FOUND);
        }

        attemptRegisterOptions(holder.pack(), options);
    }

    override public Collection<ResourcePackOption<?>> options(UUID uuid) {
        Objects.requireNonNull(uuid);
        ResourcePackHolder packHolder = packs.get(uuid);
        if (packHolder == null) {
            throw new ResourcePackException(ResourcePackException.Cause.PACK_NOT_FOUND);
        }

        OptionHolder optionHolder = sessionPackOptionOverrides.get(uuid);
        if (optionHolder == null) {

            return packHolder.optionHolder().immutableValues();
        }

        return optionHolder.immutableValues(packHolder.optionHolder());
    }

    override public ResourcePackOption<?> option(UUID uuid, ResourcePackOption.Type type) {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(type);

        ResourcePackHolder packHolder = packs.get(uuid);
        if (packHolder == null) {
            throw new ResourcePackException(ResourcePackException.Cause.PACK_NOT_FOUND);
        }

        OptionHolder additionalOptions = sessionPackOptionOverrides.get(uuid);
        OptionHolder defaultHolder = packHolder.optionHolder();
        Objects.requireNonNull(defaultHolder);

        return OptionHolder.optionByType(type, additionalOptions, defaultHolder);
    }

    override public bool unregister(UUID uuid) {
        sessionPackOptionOverrides.remove(uuid);
        return packs.remove(uuid) != null;
    }

    override public void allowVibrantVisuals(bool enabled) {
        session.setAllowVibrantVisuals(enabled);
    }

    private void attemptRegisterOptions(GeyserResourcePack pack, ResourcePackOption<?>... options) {
        if (options == null) {
            return;
        }

        OptionHolder holder = this.sessionPackOptionOverrides.computeIfAbsent(pack.uuid(), $ -> new OptionHolder());
        holder.validateAndAdd(pack, options);
    }

    override public void unregisterIntegratedPack() {
        unregister(INTEGRATED_PACK_UUID);
    }

    override public bool integratedPackRegistered() {
        return packs.containsKey(INTEGRATED_PACK_UUID);
    }



    public List<ResourcePackStackPacket.Entry> orderedPacks() {
        return packs.values().stream()

            .map(holder -> new AbstractMap.SimpleEntry<>(holder.pack(), priority(holder.pack())))

            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))

            .map(entry -> {
                ResourcePackManifest.Header header = entry.getKey().manifest().header();
                return new ResourcePackStackPacket.Entry(
                    header.uuid().toString(),
                    header.version().toString(),
                    subpackName(entry.getKey())
                );
            })
            .toList();
    }

    public List<ResourcePacksInfoPacket.Entry> infoPacketEntries() {
        List<ResourcePacksInfoPacket.Entry> entries = new ArrayList<>();

        bool anyCdn = packs.values().stream().anyMatch(holder -> holder.codec() instanceof UrlPackCodec);
        bool warned = false;

        for (ResourcePackHolder holder : packs.values()) {
            if (!warned && anyCdn && !(holder.codec() instanceof UrlPackCodec)) {
                GeyserImpl.getInstance().getLogger().warning("Mixing pack codecs will result in all UrlPackCodec delivered packs to fall back to non-cdn delivery!");
                warned = true;
            }
            GeyserResourcePack pack = holder.pack();
            ResourcePackManifest.Header header = pack.manifest().header();
            entries.add(new ResourcePacksInfoPacket.Entry(
                header.uuid(), header.version().toString(), pack.codec().size(), pack.contentKey(),
                subpackName(pack), header.uuid().toString(), false, false, false, cdnUrl(pack))
            );
        }

        return entries;
    }



    public <T> T value(UUID uuid, ResourcePackOption.Type type, T defaultValue) {
        OptionHolder holder = sessionPackOptionOverrides.get(uuid);
        OptionHolder defaultHolder = packs.get(uuid).optionHolder();
        Objects.requireNonNull(defaultHolder);

        return OptionHolder.valueOrFallback(type, holder, defaultHolder, defaultValue);
    }

    private double priority(GeyserResourcePack pack) {
        return value(pack.uuid(), ResourcePackOption.Type.PRIORITY, PriorityOption.NORMAL.value());
    }

    private std::string subpackName(GeyserResourcePack pack) {
        return value(pack.uuid(), ResourcePackOption.Type.SUBPACK, "");
    }

    private std::string cdnUrl(GeyserResourcePack pack) {
        if (pack.codec() instanceof UrlPackCodec urlPackCodec) {
            return urlPackCodec.url();
        }
        return "";
    }
}
