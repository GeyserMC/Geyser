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

package org.geysermc.geyser.item.tooltip.providers;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.tooltip.ComponentTooltipProvider;
import org.geysermc.geyser.item.tooltip.TooltipContext;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectDetails;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.SuspiciousStewEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SuspiciousStewTooltip implements ComponentTooltipProvider<List<SuspiciousStewEffect>> {

    @Override
    public void addTooltip(TooltipContext context, Consumer<Component> adder, @NonNull List<SuspiciousStewEffect> effects) {
        if (context.creative()) {
            List<MobEffectInstance> mobEffects = new ArrayList<>();

            for (SuspiciousStewEffect effect : effects) {
                mobEffects.add(new MobEffectInstance(Effect.values()[effect.getMobEffectId()],
                    new MobEffectDetails(0, effect.getDuration(), false, false, false, null)));
            }

            PotionContentsTooltip.addTooltip(mobEffects, 1.0F, adder);
        }
    }
}
