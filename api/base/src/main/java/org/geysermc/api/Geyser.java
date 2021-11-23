/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.api;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * General API class for Geyser.
 */
@NonNull
public class Geyser {
    private static GeyserApiBase api;

    /**
     * Returns the base api.
     *
     * @return the base api
     */
    public static GeyserApiBase api() {
        if (api == null) {
            throw new RuntimeException("Api has not been registered yet!");
        }

        return api;
    }

    /**
     * Returns the api of the given type.
     *
     * @param apiClass the api class
     * @param <T> the type
     * @return the api of the given type
     */
    @SuppressWarnings("unchecked")
    public static <T extends GeyserApiBase> T api(@NonNull Class<T> apiClass) {
        if (apiClass.isInstance(api)) {
            return (T) api;
        }

        if (api == null) {
            throw new RuntimeException("Api has not been registered yet!");
        } else {
            throw new RuntimeException("Api was not an instance of " + apiClass + "! Was " + api.getClass().getCanonicalName());
        }
    }

    /**
     * Registers the given api type. The api cannot be
     * registered if {@link #registered()} is true as
     * an api has already been specified.
     *
     * @param api the api
     */
    public static void set(@NonNull GeyserApiBase api) {
        if (Geyser.api != null) {
            throw new RuntimeException("Cannot redefine already registered api!");
        }

        Geyser.api = api;
    }

    /**
     * Gets if the api has been registered and
     * is ready for usage.
     *
     * @return if the api has been registered
     */
    public static boolean registered() {
        return api != null;
    }
}
