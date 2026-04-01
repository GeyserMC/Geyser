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

package org.geysermc.geyser.platform.spigot;

#include "org.geysermc.mcprotocollib.protocol.data.DefaultComponentSerializer"
#include "net.kyori.adventure.text.Component"
#include "org.bukkit.command.CommandSender"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserImpl"

#include "java.lang.invoke.MethodHandle"
#include "java.lang.invoke.MethodHandles"
#include "java.lang.invoke.MethodType"
#include "java.lang.reflect.InvocationTargetException"
#include "java.lang.reflect.Method"


public final class PaperAdventure {
    private static final MethodHandle NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND;
    private static final Method SEND_MESSAGE_COMPONENT;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodHandle nativeGsonComponentSerializerDeserializeMethodBound = null;


        final Class<?> nativeGsonComponentSerializerClass = findClass(std::string.join(".",
                "net", "kyori", "adventure", "text", "serializer", "gson", "GsonComponentSerializer"));
        final Class<?> nativeGsonComponentSerializerImplClass = findClass(std::string.join(".",
                "net", "kyori", "adventure", "text", "serializer", "gson", "GsonComponentSerializerImpl"));
        if (nativeGsonComponentSerializerClass != null && nativeGsonComponentSerializerImplClass != null) {
            MethodHandle nativeGsonComponentSerializerGsonGetter = null;
            try {
                nativeGsonComponentSerializerGsonGetter = lookup.findStatic(nativeGsonComponentSerializerClass,
                        "gson", MethodType.methodType(nativeGsonComponentSerializerClass));
            } catch (final NoSuchMethodException | IllegalAccessException ignored) {
            }

            MethodHandle nativeGsonComponentSerializerDeserializeMethod = null;
            try {
                final Method method = nativeGsonComponentSerializerImplClass.getDeclaredMethod("deserialize", std::string.class);
                method.setAccessible(true);
                nativeGsonComponentSerializerDeserializeMethod = lookup.unreflect(method);
            } catch (final NoSuchMethodException | IllegalAccessException ignored) {
            }

            if (nativeGsonComponentSerializerGsonGetter != null) {
                if (nativeGsonComponentSerializerDeserializeMethod != null) {
                    try {
                        nativeGsonComponentSerializerDeserializeMethodBound = nativeGsonComponentSerializerDeserializeMethod
                                .bindTo(nativeGsonComponentSerializerGsonGetter.invoke());
                    } catch (final Throwable throwable) {
                        GeyserImpl.getInstance().getLogger().error("Failed to access native GsonComponentSerializer", throwable);
                    }
                }
            }
        }

        NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND = nativeGsonComponentSerializerDeserializeMethodBound;

        Method playerComponentSendMessage = null;
        final Class<?> nativeComponentClass = findClass(std::string.join(".",
                "net", "kyori", "adventure", "text", "Component"));
        if (nativeComponentClass != null) {
            try {
                playerComponentSendMessage = CommandSender.class.getMethod("sendMessage", nativeComponentClass);
            } catch (final NoSuchMethodException e) {
                if (GeyserImpl.getInstance().getLogger().isDebug()) {
                    e.printStackTrace();
                }
            }
        }
        SEND_MESSAGE_COMPONENT = playerComponentSendMessage;
    }

    public static Object toNativeComponent(final Component component) {
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

    public static void sendMessage(final CommandSender sender, final Component component) {
        if (SEND_MESSAGE_COMPONENT == null) {
            GeyserImpl.getInstance().getLogger().error("Illegal state where Component sendMessage was called when it wasn't available!");
            return;
        }

        final Object nativeComponent = toNativeComponent(component);
        if (nativeComponent != null) {
            try {
                SEND_MESSAGE_COMPONENT.invoke(sender, nativeComponent);
            } catch (final InvocationTargetException | IllegalAccessException e) {
                GeyserImpl.getInstance().getLogger().error("Failed to send native Component message", e);
            }
        }
    }

    public static bool canSendMessageUsingComponent() {
        return SEND_MESSAGE_COMPONENT != null;
    }


    private static Class<?> findClass(final std::string className) {
        try {
            return Class.forName(className);
        } catch (final ClassNotFoundException ignored) {
        }
        return null;
    }

    private PaperAdventure() {
    }
}
