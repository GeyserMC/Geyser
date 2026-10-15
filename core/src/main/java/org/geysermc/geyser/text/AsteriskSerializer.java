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

package org.geysermc.geyser.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.objectmapping.meta.Processor;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.net.InetAddress;

public class AsteriskSerializer implements JsonSerializer<String> {
    public static final String[] NON_SENSITIVE_ADDRESSES = {"", "0.0.0.0", "localhost", "127.0.0.1", "auto", "unknown"};

    public static boolean showSensitive = false;

    @Override
    public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
        if (showSensitive || !isSensitiveIp(src)) {
            return new JsonPrimitive(src);
        }

        return new JsonPrimitive("***");
    }

    private static boolean isSensitiveIp(String ip) {
        for (String address : NON_SENSITIVE_ADDRESSES) {
            if (address.equalsIgnoreCase(ip)) {
                return false;
            }
        }

        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isAnyLocalAddress() || address.isSiteLocalAddress() || address.isLoopbackAddress()) {
                return false;
            }
        } catch (Exception e) {
            // Ignore
        }

        return true;
    }

    /**
     * Options for writing a config into a dump, censoring its {@link Asterisk} and {@link Secret} values.
     */
    public static ConfigurationOptions dumpOptions() {
        return InterfaceDefaultOptions.addTo(ConfigurationOptions.defaults(), builder ->
                builder.addProcessor(Asterisk.class, CONFIGURATE_SERIALIZER)
                    .addProcessor(Secret.class, String.class, CONFIGURATE_SECRET))
            .shouldCopyDefaults(false);
    }

    // Untyped, as a processor typed to String is silently skipped on a list
    private static final Processor.Factory<Asterisk, Object> CONFIGURATE_SERIALIZER = (data, fieldType) -> (value, destination) -> {
        if (showSensitive) {
            return;
        }
        if (destination.isList()) {
            destination.childrenList().forEach(AsteriskSerializer::censorIp);
        } else {
            censorIp(destination);
        }
    };

    private static void censorIp(ConfigurationNode node) {
        String value = node.getString();
        if (value == null || !isSensitiveIp(value)) {
            return;
        }
        try {
            node.set("***");
        } catch (SerializationException e) {
            throw new RuntimeException("Unable to censor IP address", e); // Error over silently printing an IP address.
        }
    }

    /**
     * Censors a value that is never safe to publish, such as a token or a password.
     * <p>
     * Unlike {@link Asterisk} this ignores {@code showSensitive}, so a full dump censors it too: a
     * dump is routinely posted to a public paste, and a reader helping with support has no use for
     * the secret itself. An empty value is left alone so that "not configured" stays visible.
     */
    private static final Processor.Factory<Secret, String> CONFIGURATE_SECRET = (data, fieldType) -> (value, destination) -> {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            destination.set("***");
        } catch (SerializationException e) {
            throw new RuntimeException("Unable to censor a secret", e); // Error over silently printing a secret.
        }
    };

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Asterisk {

    }

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Secret {

    }
}
