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

package org.geysermc.geyser.translator.sound;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks if a class should be handled as a
 * {@link SoundInteractionTranslator}.
 */
@Retention(value = RetentionPolicy.RUNTIME)
public @interface SoundTranslator {

    /**
     * The identifier(s) that the placed block must contain
     * one of. Leave empty to ignore.
     *
     * Only applies to interaction handlers that are an
     * instance of {@link BlockSoundInteractionTranslator}.
     *
     * @return the value the interacted block must contain
     */
    String[] blocks() default {};

    /**
     * The identifier(s) that the player's hand item
     * must contain one of. Leave empty to ignore.
     *
     * @return the value the item in the player's hand must contain
     */
    String[] items() default {};

    /**
     * The identifier(s) that the interacted entity must have.
     * Leave empty to ignore.
     *
     * Only applies to interaction handlers that are an
     * instance of {@link EntitySoundInteractionTranslator}.
     *
     * @return the value the item in the player's hand must contain
     */
    String[] entities() default {};

    /**
     * Controls if the interaction should still be
     * called even if the player is sneaking while
     * holding something in their hand.
     *
     * @return if the interaction should continue when player
     *         is holding something in their hand
     */
    boolean ignoreSneakingWhileHolding() default false;
}
