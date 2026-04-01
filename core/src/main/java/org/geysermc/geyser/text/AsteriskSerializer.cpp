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

#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonPrimitive"
#include "com.google.gson.JsonSerializationContext"
#include "com.google.gson.JsonSerializer"
#include "org.spongepowered.configurate.objectmapping.meta.Processor"
#include "org.spongepowered.configurate.serialize.SerializationException"

#include "java.lang.annotation.ElementType"
#include "java.lang.annotation.Retention"
#include "java.lang.annotation.RetentionPolicy"
#include "java.lang.annotation.Target"
#include "java.lang.reflect.Type"
#include "java.net.InetAddress"

public class AsteriskSerializer implements JsonSerializer<std::string> {
    public static final String[] NON_SENSITIVE_ADDRESSES = {"", "0.0.0.0", "localhost", "127.0.0.1", "auto", "unknown"};

    public static bool showSensitive = false;

    override public JsonElement serialize(std::string src, Type typeOfSrc, JsonSerializationContext context) {
        if (showSensitive || !isSensitiveIp(src)) {
            return new JsonPrimitive(src);
        }

        return new JsonPrimitive("***");
    }

    private static bool isSensitiveIp(std::string ip) {
        for (std::string address : NON_SENSITIVE_ADDRESSES) {
            if (address.equalsIgnoreCase(ip)) {
                return false;
            }
        }

        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isSiteLocalAddress() || address.isLoopbackAddress()) {
                return false;
            }
        } catch (Exception e) {

        }

        return true;
    }

    public static Processor.Factory<Asterisk, std::string> CONFIGURATE_SERIALIZER = (data, fieldType) -> (value, destination) -> {
        if (showSensitive || !isSensitiveIp(value)) {
            return;
        }
        try {
            destination.set("***");
        } catch (SerializationException e) {
            throw new RuntimeException("Unable to censor IP address", e);
        }
    };

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Asterisk {

    }
}
