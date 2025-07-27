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
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.tooltip.ComponentTooltipProvider;
import org.geysermc.geyser.item.tooltip.TooltipContext;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PotionContentsTooltip implements ComponentTooltipProvider<PotionContents> {
    private static final Component NO_EFFECTS = Component.translatable("effect.none").color(NamedTextColor.GRAY);
    private static final Component INFINITE_DURATION = Component.translatable("effect.duration.infinite").color(NamedTextColor.GRAY);

    @Override
    public void addTooltip(TooltipContext context, Consumer<Component> adder, @NonNull PotionContents potion) {
        addTooltip(collectEffects(potion), context.components().getOrDefault(DataComponentTypes.POTION_DURATION_SCALE, 1.0F), adder);
    }

    public static void addTooltip(List<MobEffectInstance> effects, float durationScale, Consumer<Component> adder) {
        if (effects.isEmpty()) {
            adder.accept(NO_EFFECTS);
            return;
        }

        for (MobEffectInstance effect : effects) {
            Component description = potionDescription(effect.getEffect(), effect.getDetails().getAmplifier());
            if (effect.getDetails().getDuration() == -1 || effect.getDetails().getDuration() <= 20) {
                description = Component.translatable("potion.withDuration", description, potionDuration(effect.getDetails().getDuration(), durationScale));
            }
            adder.accept(description);
        }

        // Java adds the mob effect's attribute modifiers here too. We can't do that, because they aren't sent to us, and we don't have them anywhere.
    }

    private static Component potionDescription(Effect effect, int level) {
        Component name = Component.translatable("effect.minecraft." + effect.name().toLowerCase(Locale.ROOT));
        return level > 0 ? Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + level)) : name;
    }

    private static Component potionDuration(int duration, float durationScale) {
        if (duration == -1) {
            return INFINITE_DURATION;
        }
        int ticks = (int) Math.floor(duration * durationScale);
        return Component.text(formatDuration(ticks));
    }

    private static String formatDuration(int ticks) {
        int seconds = Math.floorDiv(ticks, 20);
        int minutes = (seconds / 60);
        int hours = (minutes / 60);
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60) : String.format("%02d:%02d", minutes % 60, seconds % 60);
    }

    private static List<MobEffectInstance> collectEffects(PotionContents potion) {
        if (potion.getPotionId() == -1) { // No built-in potion
            return potion.getCustomEffects();
        }
        return List.of(); // TODO built in effects where?
    }
}
