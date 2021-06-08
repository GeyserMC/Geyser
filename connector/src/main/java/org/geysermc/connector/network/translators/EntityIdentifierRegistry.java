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

package org.geysermc.connector.network.translators;

import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import org.geysermc.connector.utils.FileUtils;

import java.io.InputStream;

/**
 * Registry for entity identifiers.
 */
public class EntityIdentifierRegistry {

    public static final NbtMap ENTITY_IDENTIFIERS;

    private EntityIdentifierRegistry() {
    }

    public static void init() {
        // no-op
    }

    static {
        /* Load entity identifiers */
        InputStream stream = FileUtils.getResource("bedrock/entity_identifiers.dat");

        try (NBTInputStream nbtInputStream = NbtUtils.createNetworkReader(stream)) {
            ENTITY_IDENTIFIERS = (NbtMap) nbtInputStream.readTag();
        } catch (Exception e) {
            throw new AssertionError("Unable to get entities from entity identifiers", e);
        }
    }
}
