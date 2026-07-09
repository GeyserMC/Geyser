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

package org.geysermc.geyser.platform.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.mcprotocollib.protocol.data.DefaultComponentSerializer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class for converting our shaded Adventure into the Adventure bundled in Velocity.
 * TODO THIS IS TEMPORARY UNTIL VELOCITY UPDATES TO ADVENTURE 5.x!
 */
public final class TemporaryAdventureConverter {
    private static final MethodHandle NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND;
    private static final MethodHandle NATIVE_GSON_COMPONENT_SERIALIZER_SERIALIZE_METHOD_BOUND;
    private static final MethodHandle NATIVE_LEGACY_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND;
    private static final Method SEND_MESSAGE_COMPONENT;
    private static final Method GET_MOTD;
    private static final Method SERVER_PING_BUILDER_DESCRIPTION;
    private static final Method SERVER_PING_GET_DESCRIPTION_COMPONENT;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        // String.join because otherwise the class name will be relocated
        final Class<?> nativeGsonComponentSerializerClass = findClass(String.join(".",
                "net", "kyori", "adventure", "text", "serializer", "gson", "GsonComponentSerializer"));
        final Class<?> nativeGsonComponentSerializerImplClass = findClass(String.join(".",
                "net", "kyori", "adventure", "text", "serializer", "gson", "GsonComponentSerializerImpl"));
        final Class<?> nativeComponentClass = findClass(String.join(".",
                "net", "kyori", "adventure", "text", "Component"));

        MethodHandle nativeGsonComponentSerializerDeserializeMethodBound = null;
        MethodHandle nativeGsonComponentSerializerSerializeMethodBound = null;
        if (nativeGsonComponentSerializerClass != null && nativeGsonComponentSerializerImplClass != null) {
            MethodHandle nativeGsonComponentSerializerGsonGetter = null;
            try {
                nativeGsonComponentSerializerGsonGetter = lookup.findStatic(nativeGsonComponentSerializerClass,
                        "gson", MethodType.methodType(nativeGsonComponentSerializerClass));
            } catch (final NoSuchMethodException | IllegalAccessException ignored) {
            }

            if (nativeGsonComponentSerializerGsonGetter != null) {
                try {
                    final Method deserializeMethod = nativeGsonComponentSerializerImplClass.getDeclaredMethod("deserialize", String.class);
                    deserializeMethod.setAccessible(true);
                    nativeGsonComponentSerializerDeserializeMethodBound = lookup.unreflect(deserializeMethod)
                            .bindTo(nativeGsonComponentSerializerGsonGetter.invoke());
                } catch (final Throwable throwable) {
                    GeyserImpl.getInstance().getLogger().error("Failed to access native GsonComponentSerializer#deserialize", throwable);
                }

                if (nativeComponentClass != null) {
                    try {
                        final Method serializeMethod = nativeGsonComponentSerializerImplClass.getDeclaredMethod("serialize", nativeComponentClass);
                        serializeMethod.setAccessible(true);
                        nativeGsonComponentSerializerSerializeMethodBound = lookup.unreflect(serializeMethod)
                                .bindTo(nativeGsonComponentSerializerGsonGetter.invoke());
                    } catch (final Throwable throwable) {
                        GeyserImpl.getInstance().getLogger().error("Failed to access native GsonComponentSerializer#serialize", throwable);
                    }
                }
            }
        }
        NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND = nativeGsonComponentSerializerDeserializeMethodBound;
        NATIVE_GSON_COMPONENT_SERIALIZER_SERIALIZE_METHOD_BOUND = nativeGsonComponentSerializerSerializeMethodBound;

        // legacySection()#deserialize(String) is declared directly on the public LegacyComponentSerializer
        // interface (unlike Gson's, whose deserialize(String) lives on a package-private impl class), so no
        // setAccessible dance is needed here - a plain MethodHandle lookup works.
        final Class<?> nativeLegacyComponentSerializerClass = findClass(String.join(".",
                "net", "kyori", "adventure", "text", "serializer", "legacy", "LegacyComponentSerializer"));
        final Class<?> nativeTextComponentClass = findClass(String.join(".",
                "net", "kyori", "adventure", "text", "TextComponent"));

        MethodHandle nativeLegacyComponentSerializerDeserializeMethodBound = null;
        if (nativeLegacyComponentSerializerClass != null && nativeTextComponentClass != null) {
            try {
                final MethodHandle legacySectionGetter = lookup.findStatic(nativeLegacyComponentSerializerClass,
                        "legacySection", MethodType.methodType(nativeLegacyComponentSerializerClass));
                final MethodHandle deserializeMethod = lookup.findVirtual(nativeLegacyComponentSerializerClass,
                        "deserialize", MethodType.methodType(nativeTextComponentClass, String.class));
                nativeLegacyComponentSerializerDeserializeMethodBound = deserializeMethod.bindTo(legacySectionGetter.invoke());
            } catch (final Throwable throwable) {
                GeyserImpl.getInstance().getLogger().error("Failed to access native LegacyComponentSerializer#deserialize", throwable);
            }
        }
        NATIVE_LEGACY_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND = nativeLegacyComponentSerializerDeserializeMethodBound;

        Method sendMessageComponent = null;
        Method serverPingBuilderDescription = null;
        Method serverPingGetDescriptionComponent = null;
        if (nativeComponentClass != null) {
            try {
                sendMessageComponent = CommandSource.class.getMethod("sendMessage", nativeComponentClass);
            } catch (final NoSuchMethodException e) {
                if (GeyserImpl.getInstance().getLogger().isDebug()) {
                    e.printStackTrace();
                }
            }

            try {
                serverPingBuilderDescription = ServerPing.Builder.class.getMethod("description", nativeComponentClass);
            } catch (final NoSuchMethodException e) {
                if (GeyserImpl.getInstance().getLogger().isDebug()) {
                    e.printStackTrace();
                }
            }

            try {
                serverPingGetDescriptionComponent = ServerPing.class.getMethod("getDescriptionComponent");
            } catch (final NoSuchMethodException e) {
                if (GeyserImpl.getInstance().getLogger().isDebug()) {
                    e.printStackTrace();
                }
            }
        }
        SEND_MESSAGE_COMPONENT = sendMessageComponent;
        SERVER_PING_BUILDER_DESCRIPTION = serverPingBuilderDescription;
        SERVER_PING_GET_DESCRIPTION_COMPONENT = serverPingGetDescriptionComponent;

        Method getMotd = null;
        try {
            getMotd = ProxyConfig.class.getMethod("getMotd");
        } catch (final NoSuchMethodException e) {
            if (GeyserImpl.getInstance().getLogger().isDebug()) {
                e.printStackTrace();
            }
        }
        GET_MOTD = getMotd;
    }

    /**
     * Sends one of our (shaded) Components to a Velocity CommandSource, going through its native Adventure.
     */
    public static void sendMessage(final CommandSource target, final Component component) {
        if (SEND_MESSAGE_COMPONENT == null) {
            GeyserImpl.getInstance().getLogger().error("Illegal state where Component sendMessage was called when it wasn't available!");
            return;
        }

        final Object nativeComponent = toNativeComponentFromLegacyString(component);
        if (nativeComponent != null) {
            try {
                SEND_MESSAGE_COMPONENT.invoke(target, nativeComponent);
            } catch (final InvocationTargetException | IllegalAccessException e) {
                GeyserImpl.getInstance().getLogger().error("Failed to send native Component message", e);
            }
        }
    }

    /**
     * Converts one of our (shaded) Components into an unrelocated Component.
     */
    public static @Nullable Object toNativeComponentFromLegacyString(final Component component) {
        if (NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND == null) {
            GeyserImpl.getInstance().getLogger().error("Illegal state where Component serialization was called when it wasn't available!");
            return null;
        }

        try {
            return NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND.invoke(DefaultComponentSerializer.get().serialize(component));
        } catch (final Throwable throwable) {
            GeyserImpl.getInstance().getLogger().error("Failed to create native Component message", throwable);
            return null;
        }
    }

    public static void sendMessage(final CommandSource target, final String message) {
        if (SEND_MESSAGE_COMPONENT == null) {
            GeyserImpl.getInstance().getLogger().error("Illegal state where Component sendMessage was called when it wasn't available!");
            return;
        }

        final Object nativeComponent = toNativeComponentFromLegacyString(message);
        if (nativeComponent != null) {
            try {
                SEND_MESSAGE_COMPONENT.invoke(target, nativeComponent);
            } catch (final InvocationTargetException | IllegalAccessException e) {
                GeyserImpl.getInstance().getLogger().error("Failed to send native Component message", e);
            }
        }
    }

    /**
     * Converts legacy-formatted text directly into an unrelocated Component
     */
    public static @Nullable Object toNativeComponentFromLegacyString(final String message) {
        if (NATIVE_LEGACY_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND == null) {
            GeyserImpl.getInstance().getLogger().error("Illegal state where legacy Component deserialization was called when it wasn't available!");
            return null;
        }

        try {
            return NATIVE_LEGACY_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND.invoke(message);
        } catch (final Throwable throwable) {
            GeyserImpl.getInstance().getLogger().error("Failed to create native Component message from legacy text", throwable);
            return null;
        }
    }

    /**
     * Serializes a native (unrelocated) Component - e.g. one obtained from Velocity's own API - into json.
     */
    public static @Nullable String toJson(final @Nullable Object nativeComponent) {
        if (nativeComponent == null) {
            return null;
        }
        if (NATIVE_GSON_COMPONENT_SERIALIZER_SERIALIZE_METHOD_BOUND == null) {
            GeyserImpl.getInstance().getLogger().error("Illegal state where native Component serialization was called when it wasn't available!");
            return null;
        }

        try {
            return (String) NATIVE_GSON_COMPONENT_SERIALIZER_SERIALIZE_METHOD_BOUND.invoke(nativeComponent);
        } catch (final Throwable throwable) {
            GeyserImpl.getInstance().getLogger().error("Failed to serialize native Component message", throwable);
            return null;
        }
    }

    /**
     * Gets the configured MOTD as a native (unrelocated) Component.
     */
    public static @Nullable Object motd(final ProxyConfig config) {
        if (GET_MOTD == null) {
            GeyserImpl.getInstance().getLogger().error("Illegal state where native motd was requested when it wasn't available!");
            return null;
        }

        try {
            return GET_MOTD.invoke(config);
        } catch (final InvocationTargetException | IllegalAccessException e) {
            GeyserImpl.getInstance().getLogger().error("Failed to get native motd Component", e);
            return null;
        }
    }

    /**
     * Sets a {@link ServerPing.Builder}'s description from a native (unrelocated) Component.
     */
    public static ServerPing.Builder description(final ServerPing.Builder builder, final @Nullable Object nativeComponent) {
        if (nativeComponent == null) {
            return builder;
        }
        if (SERVER_PING_BUILDER_DESCRIPTION == null) {
            GeyserImpl.getInstance().getLogger().error("Illegal state where ServerPing description was set when it wasn't available!");
            return builder;
        }

        try {
            return (ServerPing.Builder) SERVER_PING_BUILDER_DESCRIPTION.invoke(builder, nativeComponent);
        } catch (final InvocationTargetException | IllegalAccessException e) {
            GeyserImpl.getInstance().getLogger().error("Failed to set native description Component", e);
            return builder;
        }
    }

    /**
     * Gets a {@link ServerPing}'s description as a native (unrelocated) Component.
     */
    public static @Nullable Object getDescriptionComponent(final ServerPing ping) {
        if (SERVER_PING_GET_DESCRIPTION_COMPONENT == null) {
            GeyserImpl.getInstance().getLogger().error("Illegal state where ServerPing description was requested when it wasn't available!");
            return null;
        }

        try {
            return SERVER_PING_GET_DESCRIPTION_COMPONENT.invoke(ping);
        } catch (final InvocationTargetException | IllegalAccessException e) {
            GeyserImpl.getInstance().getLogger().error("Failed to get native description Component", e);
            return null;
        }
    }

    /**
     * Gets a class by the first name available.
     *
     * @return a class or {@code null} if not found
     */
    private static @Nullable Class<?> findClass(final String className) {
        try {
            return Class.forName(className);
        } catch (final ClassNotFoundException ignored) {
        }
        return null;
    }

    private TemporaryAdventureConverter() {
    }
}
