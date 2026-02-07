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

package org.geysermc.geyser.registry.loader;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.api.bedrock.camera.CameraFade;
import org.geysermc.geyser.api.bedrock.camera.CameraPosition;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.api.network.NetworkChannel;
import org.geysermc.geyser.api.network.PacketChannel;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.api.network.message.MessageCodec;
import org.geysermc.geyser.api.pack.PathPackCodec;
import org.geysermc.geyser.api.pack.UrlPackCodec;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.api.pack.option.SubpackOption;
import org.geysermc.geyser.api.pack.option.UrlFallbackOption;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.event.GeyserEventRegistrar;
import org.geysermc.geyser.extension.command.GeyserExtensionCommand;
import org.geysermc.geyser.impl.ExtensionIdentifierImpl;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.impl.camera.GeyserCameraFade;
import org.geysermc.geyser.impl.camera.GeyserCameraPosition;
import org.geysermc.geyser.item.GeyserCustomItemData;
import org.geysermc.geyser.item.GeyserCustomItemOptions;
import org.geysermc.geyser.item.GeyserNonVanillaCustomItemData;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents;
import org.geysermc.geyser.level.block.GeyserCustomBlockData;
import org.geysermc.geyser.level.block.GeyserGeometryComponent;
import org.geysermc.geyser.level.block.GeyserJavaBlockState;
import org.geysermc.geyser.level.block.GeyserMaterialInstance;
import org.geysermc.geyser.level.block.GeyserNonVanillaCustomBlockData;
import org.geysermc.geyser.network.ExtensionNetworkChannel;
import org.geysermc.geyser.network.ExternalNetworkChannel;
import org.geysermc.geyser.network.PacketChannelImpl;
import org.geysermc.geyser.network.message.BedrockPacketMessage;
import org.geysermc.geyser.network.message.ByteBufCodec;
import org.geysermc.geyser.network.message.JavaPacketMessage;
import org.geysermc.geyser.pack.option.GeyserPriorityOption;
import org.geysermc.geyser.pack.option.GeyserSubpackOption;
import org.geysermc.geyser.pack.option.GeyserUrlFallbackOption;
import org.geysermc.geyser.pack.path.GeyserPathPackCodec;
import org.geysermc.geyser.pack.url.GeyserUrlPackCodec;
import org.geysermc.geyser.registry.provider.ProviderSupplier;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registers the provider data from the provider.
 */
public class ProviderRegistryLoader implements RegistryLoader<Map<Class<?>, ProviderSupplier>, Map<Class<?>, ProviderSupplier>> {

    @Override
    public Map<Class<?>, ProviderSupplier> load(Map<Class<?>, ProviderSupplier> providers) {
        // misc
        providers.put(Identifier.class, args -> IdentifierImpl.of((String) args[0], (String) args[1]));

        // commands
        providers.put(Command.Builder.class, args -> new GeyserExtensionCommand.Builder<>((Extension) args[0]));

        // custom blocks
        providers.put(CustomBlockComponents.Builder.class, args -> new GeyserCustomBlockComponents.Builder());
        providers.put(CustomBlockData.Builder.class, args -> new GeyserCustomBlockData.Builder());
        providers.put(JavaBlockState.Builder.class, args -> new GeyserJavaBlockState.Builder());
        providers.put(NonVanillaCustomBlockData.Builder.class, args -> new GeyserNonVanillaCustomBlockData.Builder());
        providers.put(MaterialInstance.Builder.class, args -> new GeyserMaterialInstance.Builder());
        providers.put(GeometryComponent.Builder.class, args -> new GeyserGeometryComponent.Builder());

        // misc
        providers.put(EventRegistrar.class, args -> new GeyserEventRegistrar(args[0]));

        // packs
        providers.put(PathPackCodec.class, args -> new GeyserPathPackCodec((Path) args[0]));
        providers.put(UrlPackCodec.class, args -> new GeyserUrlPackCodec((String) args[0]));
        providers.put(PriorityOption.class, args -> new GeyserPriorityOption((int) args[0]));
        providers.put(SubpackOption.class, args -> new GeyserSubpackOption((String) args[0]));
        providers.put(UrlFallbackOption.class, args -> new GeyserUrlFallbackOption((Boolean) args[0]));

        // items
        providers.put(CustomItemData.Builder.class, args -> new GeyserCustomItemData.Builder());
        providers.put(CustomItemOptions.Builder.class, args -> new GeyserCustomItemOptions.Builder());
        providers.put(NonVanillaCustomItemData.Builder.class, args -> new GeyserNonVanillaCustomItemData.Builder());

        // cameras
        providers.put(CameraFade.Builder.class, args -> new GeyserCameraFade.Builder());
        providers.put(CameraPosition.Builder.class, args -> new GeyserCameraPosition.Builder());

        // network api
        providers.put(Message.PacketWrapped.class, args -> {
            if (args.length < 1) {
                throw new IllegalArgumentException("Message.PacketWrapped requires at least one argument, got " + args.length);
            }

            if (args[0] instanceof BedrockPacket bedrockPacket) {
                return new BedrockPacketMessage<>(bedrockPacket);
            } else if (args[0] instanceof MinecraftPacket javaPacket) {
                return new JavaPacketMessage<>(javaPacket);
            } else {
                throw new IllegalArgumentException("Unsupported packet type: " + args[0].getClass().getName());
            }
        });

        providers.put(MessageCodec.class, args -> {
            if (args.length < 1) {
                throw new IllegalArgumentException("MessageCodec requires at least one argument, got " + args.length);
            }

            Set<MessageCodec.EncoderOptions> options = new HashSet<>();
            if (args.length > 1 && args[1] instanceof MessageCodec.EncoderOptions[] encoderOptions) {
                options = new HashSet<>(Arrays.asList(encoderOptions));
            }

            if (args[0] instanceof Class<?> wrapperType) {
                if (wrapperType == ByteBuf.class) {
                    if (options.contains(MessageCodec.EncoderOptions.LITTLE_ENDIAN)) {
                        return ByteBufCodec.INSTANCE_LE;
                    } else {
                        return ByteBufCodec.INSTANCE;
                    }
                }
            }

            throw new IllegalArgumentException("Unsupported codec type: " + args[0]);
        });

        providers.put(NetworkChannel.class, args -> {
            // Extension network channel
            if (args.length == 3 && args[0] instanceof Extension extension && args[1] instanceof String channel && args[2] instanceof Class<?> messageType) {
                return new ExtensionNetworkChannel(extension, channel, messageType);
            } else if (args.length == 2 && args[0] instanceof Identifier identifier && args[1] instanceof Class<?> messageType) {
                // External network channel
                return new ExternalNetworkChannel(identifier, messageType);
            } else {
                throw new IllegalArgumentException("Unknown arguments provided for NetworkChannel provider. " +
                        "Could not create a channel given the arguments: " + Arrays.toString(args));
            }
        });

        providers.put(PacketChannel.class, args -> {
            if (args.length < 4) {
                throw new IllegalArgumentException("PacketChannel requires at least four arguments, got " + args.length);
            }

            if (args[0] instanceof Extension extension && args[1] instanceof String platform && args[2] instanceof Integer packetId
                    && args[3] instanceof Class<?> packetType) {
                return switch (platform) {
                    case "java" ->
                            new PacketChannelImpl(new ExtensionIdentifierImpl(extension, "java_packet_" + packetId), true, packetId, packetType);
                    case "bedrock" ->
                            new PacketChannelImpl(new ExtensionIdentifierImpl(extension, "bedrock_packet_" + packetId), false, packetId, packetType);
                    default -> throw new IllegalArgumentException("Unknown platform type for PacketChannel: " + platform);
                };
            }

            throw new IllegalArgumentException("Unknown arguments provided for PacketChannel provider. " +
                    "Could not create a channel given the arguments: " + Arrays.toString(args));
        });

        return providers;
    }
}
