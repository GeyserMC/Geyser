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

package org.geysermc.geyser.registry.mappings.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.block.custom.CustomBlockData;

import java.util.Map;

/**
 * This class is used to store a custom block mappings, which contain all of the 
 * data required to register a custom block that overrides a group of java block
 * states.
 * 
 * @param data The custom block data
 * @param states The custom block state mappings
 * @param javaIdentifier The java identifier of the block
 * @param overrideItem Whether the custom block should override the java item
 */
public record CustomBlockMapping(@NonNull CustomBlockData data, @NonNull Map<String, CustomBlockStateMapping> states, @NonNull String javaIdentifier, boolean overrideItem) {
}
