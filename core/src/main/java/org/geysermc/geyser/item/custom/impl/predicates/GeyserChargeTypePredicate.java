package org.geysermc.geyser.item.custom.impl.predicates;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ChargedProjectile;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.item.ChargeTypePredicate;
import org.geysermc.geyser.impl.GeyserCoreProvided;

import java.util.Objects;

@GeyserCoreProvided
public record GeyserChargeTypePredicate(ChargedProjectile.ChargeType type, boolean negated) implements ChargeTypePredicate {

    public GeyserChargeTypePredicate {
        Objects.requireNonNull(type, "charge type cannot be null");
    }

    @Override
    public boolean test(ItemPredicateContext context) {
        return negated != context.chargedProjectiles().stream().anyMatch(projectile -> projectile.type() == this.type);
    }

    @Override
    public @NonNull MinecraftPredicate<ItemPredicateContext> negate() {
        return new GeyserChargeTypePredicate(type, !negated);
    }
}
