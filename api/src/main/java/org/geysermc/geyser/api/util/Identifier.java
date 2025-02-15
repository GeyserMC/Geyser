/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.util;

import java.util.Objects;
import java.util.function.Function;

public final class Identifier {
    public static final String DEFAULT_NAMESPACE = "minecraft";
    private final String namespace;
    private final String path;

    private Identifier(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
        validate();
    }

    public static Identifier of(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    public static Identifier of(String identifier) {
        String[] split = identifier.split(":");
        String namespace;
        String path;
        if (split.length == 1) {
            namespace = DEFAULT_NAMESPACE;
            path = split[0];
        } else if (split.length == 2) {
            namespace = split[0];
            path = split[1];
        } else {
            throw new IllegalArgumentException("':' in identifier path: " + identifier);
        }
        return new Identifier(namespace, path);
    }

    private void validate() {
        checkString(namespace, "namespace", Identifier::allowedInNamespace);
        checkString(path, "path", Identifier::allowedInPath);
    }

    public String namespace() {
        return namespace;
    }

    public String path() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Identifier other = (Identifier) o;
        return Objects.equals(namespace, other.namespace) && Objects.equals(path, other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    private static void checkString(String string, String type, Function<Character, Boolean> characterChecker) {
        for (int i = 0; i < string.length(); i++) {
            if (!characterChecker.apply(string.charAt(i))) {
                throw new IllegalArgumentException("Illegal character in " + type + " " + string);
            }
        }
    }

    private static boolean allowedInNamespace(char character) {
        return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.';
    }

    private static boolean allowedInPath(char character) {
        return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.' || character == '/';
    }
}
