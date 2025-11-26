/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity;

import org.geysermc.geyser.impl.IdentifierImpl;

/**
 * Most Bedrock entities are registered in {@link VanillaEntities} - however, some are
 * done here to be able to re-use the same bedrock entity across multiple Java types
 */
public class BedrockEntityDefinitions {
    public static final BedrockEntityDefinition ARMOR_STAND;
    public static final BedrockEntityDefinition ARROW;
    public static final BedrockEntityDefinition BOAT;
    public static final BedrockEntityDefinition CHEST_BOAT;
    public static final BedrockEntityDefinition EVOCATION_ILLAGER;
    public static final BedrockEntityDefinition LLAMA;
    public static final BedrockEntityDefinition MINECART;
    public static final BedrockEntityDefinition SPLASH_POTION;
    public static final BedrockEntityDefinition ZOMBIE;

    static {
        ARMOR_STAND = BedrockEntityDefinition.of(IdentifierImpl.of("armor_stand"));
        ARROW = BedrockEntityDefinition.of(IdentifierImpl.of("arrow"));
        BOAT = BedrockEntityDefinition.of(IdentifierImpl.of("boat"));
        CHEST_BOAT = BedrockEntityDefinition.of(IdentifierImpl.of("chest_boat"));
        EVOCATION_ILLAGER = BedrockEntityDefinition.of(IdentifierImpl.of("evocation_illager"));
        LLAMA = BedrockEntityDefinition.of(IdentifierImpl.of("llama"));
        MINECART = BedrockEntityDefinition.of(IdentifierImpl.of("minecraft"));
        SPLASH_POTION = BedrockEntityDefinition.of(IdentifierImpl.of("splash_position"));
        ZOMBIE = BedrockEntityDefinition.of(IdentifierImpl.of("zombie"));
    }

    public static void init() {
        // no-op
    }

}
