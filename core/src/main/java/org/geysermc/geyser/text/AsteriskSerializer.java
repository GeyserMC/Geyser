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

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.Serial;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

public class AsteriskSerializer extends StdSerializer<Object> implements ContextualSerializer {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String[] NON_SENSITIVE_ADDRESSES = {"", "0.0.0.0", "localhost", "127.0.0.1", "auto", "unknown"};

    public static boolean showSensitive = false;

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonSerialize(using = AsteriskSerializer.class)
    public @interface Asterisk {
        String value() default "***";
        /**
         * If true, this value will be shown if {@link #showSensitive} is true, or if the IP is determined to not be a public IP
         * 
         * @return true if this should be analyzed and treated as an IP
         */
        boolean isIp() default false;
    }

    String asterisk;
    boolean isIp;

    @SuppressWarnings("unused") // Used by Jackson for Geyser dumps
    public AsteriskSerializer() {
        super(Object.class);
    }

    public AsteriskSerializer(String asterisk, boolean isIp) {
        super(Object.class);
        this.asterisk = asterisk;
        this.isIp = isIp;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializerProvider, BeanProperty property) {
        Optional<Asterisk> anno = Optional.ofNullable(property)
                .map(prop -> prop.getAnnotation(Asterisk.class));

        return new AsteriskSerializer(anno.map(Asterisk::value).orElse(null), anno.map(Asterisk::isIp).orElse(false));
    }

    @Override
    public void serialize(Object obj, JsonGenerator gen, SerializerProvider prov) throws IOException {
        if (isIp && (showSensitive || !isSensitiveIp((String) obj))) {
            gen.writeObject(obj);
            return;
        }

        gen.writeString(asterisk);
    }

    private boolean isSensitiveIp(String ip) {
        for (String address : NON_SENSITIVE_ADDRESSES) {
            if (address.equalsIgnoreCase(ip)) {
                return false;
            }
        }
        return true;
    }
}
