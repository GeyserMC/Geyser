/*
 * This file is part of adventure-platform, licensed under the MIT License.
 *
 * Copyright (c) 2018-2020 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.geysermc.connector.network.translators.chat;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer;
import net.kyori.adventure.util.Codec;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.UUID;

/**
 * This class is borrowed from adventure-platform
 * https://github.com/KyoriPowered/adventure-platform/blob/master/text-serializer-craftbukkit/src/main/java/net/kyori/adventure/text/serializer/craftbukkit/NBTLegacyHoverEventSerializer.java
 */
final class NBTLegacyHoverEventSerializer implements LegacyHoverEventSerializer {
    static final NBTLegacyHoverEventSerializer INSTANCE = new NBTLegacyHoverEventSerializer();
    private static final TagStringIO SNBT_IO = TagStringIO.get();
    private static final Codec<CompoundBinaryTag, String, IOException, IOException> SNBT_CODEC = Codec.of(SNBT_IO::asCompound, SNBT_IO::asString);

    static final String ITEM_TYPE = "id";
    static final String ITEM_COUNT = "Count";
    static final String ITEM_TAG = "tag";

    static final String ENTITY_NAME = "name";
    static final String ENTITY_TYPE = "type";
    static final String ENTITY_ID = "id";

    private NBTLegacyHoverEventSerializer() {
    }

    @Override
    public HoverEvent.@NonNull ShowItem deserializeShowItem(final @NonNull Component input) throws IOException {
        if(!(input instanceof TextComponent)) {
            throw new IllegalArgumentException("Legacy events must be single Component instances");
        }
        final CompoundBinaryTag contents = SNBT_CODEC.decode(((TextComponent) input).content());
        final CompoundBinaryTag tag = contents.getCompound(ITEM_TAG);
        return HoverEvent.ShowItem.of(
                Key.key(contents.getString(ITEM_TYPE)),
                contents.getByte(ITEM_COUNT, (byte) 1),
                tag == CompoundBinaryTag.empty() ? null : BinaryTagHolder.encode(tag, SNBT_CODEC)
        );
    }

    @Override
    public HoverEvent.@NonNull ShowEntity deserializeShowEntity(final @NonNull Component input, final Codec.Decoder<Component, String, ? extends RuntimeException> componentCodec) throws IOException {
        if(!(input instanceof TextComponent)) {
            throw new IllegalArgumentException("Legacy events must be single Component instances");
        }
        final CompoundBinaryTag contents = SNBT_CODEC.decode(((TextComponent) input).content());
        return HoverEvent.ShowEntity.of(
                Key.key(contents.getString(ENTITY_TYPE)),
                UUID.fromString(contents.getString(ENTITY_ID)),
                componentCodec.decode(contents.getString(ENTITY_NAME))
        );
    }

    @Override
    public @NonNull Component serializeShowItem(final HoverEvent.@NonNull ShowItem input) throws IOException {
        final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder()
                .putString(ITEM_TYPE, input.item().asString())
                .putByte(ITEM_COUNT, (byte) input.count());
        if(input.nbt() != null) {
            builder.put(ITEM_TAG, input.nbt().get(SNBT_CODEC));
        }

        return Component.text(SNBT_CODEC.encode(builder.build()));
    }

    @Override
    public @NonNull Component serializeShowEntity(final HoverEvent.@NonNull ShowEntity input, final Codec.Encoder<Component, String, ? extends RuntimeException> componentCodec) throws IOException {
        final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder()
                .putString(ENTITY_ID, input.id().toString())
                .putString(ENTITY_TYPE, input.type().asString());
        if(input.name() != null) {
            builder.putString(ENTITY_NAME, componentCodec.encode(input.name()));
        }
        return Component.text(SNBT_CODEC.encode(builder.build()));
    }
}